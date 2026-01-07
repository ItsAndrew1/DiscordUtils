//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerHeadsGUIs implements Listener{
    private final DiscordUtils plugin;
    private OfflinePlayer clickedPlayer;

    public PlayerHeadsGUIs(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player, int page) throws SQLException {
        int inventorySize = 54;
        String invTitle = "Select Player (page "+page+")";
        Inventory gui = Bukkit.createInventory(null, inventorySize, invTitle);

        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        int headsPerPage = 35;
        int startIndex = (page-1)*headsPerPage;
        int endIndex = Math.min(startIndex + headsPerPage, players.length);

        //Setting the navigation buttons
        if(page > 1) gui.setItem(48, createButton(Material.ARROW, ChatColor.RED + "◀ Previous Page"));
        if(endIndex < players.length) gui.setItem(50, createButton(Material.ARROW, ChatColor.GREEN + "Next Page ▶"));

        //Adding each player's skull
        for(int i = startIndex; i < endIndex; i++){
            OfflinePlayer targetPlayer = players[i];

            ItemStack skull =  new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

            skullMeta.setOwningPlayer(targetPlayer);
            skullMeta.setDisplayName(ChatColor.YELLOW + targetPlayer.getName());

            //Setting the lore
            List<String> lore = new ArrayList<>();
            String playerStatus;

            if(targetPlayer.isOnline()) playerStatus = ChatColor.translateAlternateColorCodes('&', "&aStatus: &a&lONLINE");
            else if(targetPlayer.isBanned()) playerStatus = ChatColor.translateAlternateColorCodes('&', "&aStatus: &5&lBANNED");
            else playerStatus = ChatColor.translateAlternateColorCodes('&', "&aStatus: &c&lOFFLINE");

            int activePunishments = plugin.getDatabaseManager().getPlayerActivePunishmentsNr(targetPlayer.getUniqueId());
            int inactivePunishments = plugin.getDatabaseManager().getPlayerExpiredPunishmentsNr(targetPlayer.getUniqueId());

            lore.add(" ");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&aActive Punishments: &c&l"+activePunishments));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&aExpired Punishments: &c&l"+inactivePunishments));
            lore.add(" ");
            lore.add(playerStatus);

            skullMeta.setLore(lore);
            skull.setItemMeta(skullMeta);

            gui.addItem(skull);
        }

        //Setting the search player button
        gui.setItem(53, createButton(Material.OAK_SIGN, ChatColor.YELLOW + "Search Player"));

        //Setting the exit button
        gui.setItem(49, createButton(Material.RED_CONCRETE, ChatColor.translateAlternateColorCodes('&', "&c&lEXIT")));

        //Setting decoration glass
        ItemStack decoGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = decoGlass.getItemMeta();
        meta.setDisplayName(" ");
        decoGlass.setItemMeta(meta);
        for(int i = 36; i<=44; i++) gui.setItem(i, decoGlass);

        player.openInventory(gui);
    }

    private ItemStack createButton(Material mat, String title){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        item.setItemMeta(meta);
        return item;
    }

    private int getPageFromTitle(String title){
        String page = title.replaceAll("[^0-9]", "");
        return Integer.parseInt(page)-1;
    }

    public OfflinePlayer getClickedPlayer() {
        return clickedPlayer;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) throws SQLException {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().contains("Select Player")) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if(item == null || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        ItemMeta meta = item.getItemMeta();
        Material clickedMaterial = item.getType();

        //If the staff clicks on a player head
        if(clickedMaterial == Material.PLAYER_HEAD){
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            clickedPlayer = skullMeta.getOwningPlayer();

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getAddRemovePunishGUI().showGui(player);
            return;
        }

        //If the staff clicks on exit item
        if(clickedMaterial.equals(Material.RED_CONCRETE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        //If the staff clicks on player search
        if(clickedMaterial.equals(Material.OAK_SIGN)){
            String chatPrefix = plugin.getConfig().getString("chat-prefix");

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the &lexact name &aof the player:"));

            plugin.waitForPlayerInput(player, input->{
                    OfflinePlayer inputPlayer;
                    try{
                        inputPlayer = Bukkit.getOfflinePlayer(input);
                    } catch(Exception e){
                        throw new RuntimeException();
                    }

                    if(!inputPlayer.hasPlayedBefore()){
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cUnknown player with name &l"+input+"&c!"));

                        //Reopens the gui after 1/2 seconds
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    showGui(player, 1);
                                } catch (SQLException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }.runTaskLater(plugin, 10L);
                        return;
                    }

                    if(inputPlayer == player){
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou cannot search yourself!"));

                        //Reops the gui after 1/2 seconds
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                try {
                                    showGui(player, 1);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }.runTaskLater(plugin, 10L);
                        return;
                    }

                    clickedPlayer = inputPlayer;
                    plugin.getAddRemovePunishGUI().showGui(player);
            });
        }

        //If the staff clicks on previous page button
        if(meta.getDisplayName().contains(ChatColor.RED + "Previous")){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            showGui(player, getPageFromTitle(event.getView().getTitle())+1);
            return;
        }

        //If the staff clicks on next page button
        if(meta.getDisplayName().contains(ChatColor.GREEN + "Next")){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            showGui(player, getPageFromTitle(event.getView().getTitle())-1);
            return;
        }
    }
}
