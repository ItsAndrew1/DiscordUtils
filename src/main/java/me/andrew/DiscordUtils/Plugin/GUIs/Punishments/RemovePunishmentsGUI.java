//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.Punishment;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class RemovePunishmentsGUI implements Listener {
    private final DiscordUtils plugin;

    public RemovePunishmentsGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player, int page) throws SQLException {
        int invSize = 54;
        String title = "Remove a Punishment (Page "+page+")";
        Inventory gui = Bukkit.createInventory(player, invSize, title);

        //Return button
        ItemStack returnButton = createButton(Material.SPECTRAL_ARROW, ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
        gui.setItem(49, returnButton);

        //Deco glass
        ItemStack decoGlass = createButton(Material.BLACK_STAINED_GLASS_PANE, " ");
        for(int i = 9; i<=17; i++) gui.setItem(i, decoGlass);

        //Player Head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(plugin.getPlayerHeadsGUIs().getClickedPlayer());
        headMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e"+plugin.getPlayerHeadsGUIs().getClickedPlayer().getName()));
        playerHead.setItemMeta(headMeta);
        gui.setItem(4, playerHead);

        //If the player does not have any punishments
        OfflinePlayer targetPlayer = plugin.getPlayerHeadsGUIs().getClickedPlayer();
        if(!plugin.getDatabaseManager().playerHasPunishments(targetPlayer.getUniqueId())) gui.setItem(31, createButton(Material.BARRIER, ChatColor.translateAlternateColorCodes('&', "&cPlayer &e"+targetPlayer.getName()+" &cdoesn't have any active punishments at the moment!")));

        //If the player only has warns, still create the no punishments item
        else if(playerHasOnlyWarns(targetPlayer)) gui.setItem(31, createButton(Material.BARRIER, ChatColor.translateAlternateColorCodes('&', "&cPlayer &e"+targetPlayer.getName()+" &cdoesn't have any active punishments at the moment!")));

        else{
            //Displaying the punishments
            int punishmentsPerPage = 27;
            int offset = (page - 1) * punishmentsPerPage;
            List<Punishment> playerActivePunishments = plugin.getDatabaseManager().getPlayerPunishments(targetPlayer.getUniqueId(), PunishmentsFilter.ACTIVE, punishmentsPerPage, offset);

            int startSlot = 18;
            for(int i = 0; i<playerActivePunishments.size(); i++){
                if(!playerActivePunishments.get(i).getPunishmentType().name().contains("WARN")) gui.setItem(startSlot + i,  createPunishmentItem(playerActivePunishments.get(i)));
            }

            //Page Navigation Buttons
            if(page > 1) gui.setItem(48, createButton(Material.ARROW, ChatColor.translateAlternateColorCodes('&', ChatColor.RED + "◀ Previous Page")));
            if(playerActivePunishments.size() >= punishmentsPerPage) gui.setItem(50, createButton(Material.ARROW, ChatColor.GREEN + "Next Page ▶"));
        }

        player.openInventory(gui);
    }

    private ItemStack createButton(Material mat, String displayName){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack createPunishmentItem(Punishment p){
        //Setting the material based on the punishment type
        Material punishmentMaterial = switch(p.getPunishmentType()){
            case PunishmentType.KICK -> Material.LEATHER_BOOTS;
            case PunishmentType.PERM_BAN -> Material.NETHERITE_AXE;
            case PunishmentType.TEMP_BAN -> Material.IRON_AXE;
            case PunishmentType.PERM_MUTE -> Material.SOUL_LANTERN;
            case PunishmentType.TEMP_MUTE -> Material.LANTERN;
            default -> null;
        };

        ItemStack punishmentItem = new ItemStack(punishmentMaterial);
        ItemMeta piMeta = punishmentItem.getItemMeta();

        //Setting the display name based on the punishment type
        String displayName = switch(p.getPunishmentType()){
            case PunishmentType.PERM_BAN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT BAN");
            case PunishmentType.TEMP_BAN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY BAN");
            case PunishmentType.KICK -> ChatColor.translateAlternateColorCodes('&', "&e&lKICK");
            case PunishmentType.PERM_MUTE -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT MUTE");
            case PunishmentType.TEMP_MUTE -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY MUTE");
            default -> null;
        };
        piMeta.setDisplayName(displayName);

        //Formatting the time when the punishment was created
        long issuedAt = p.getIssuedAt();
        Instant createdInstant = Instant.ofEpochMilli(issuedAt);
        LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String formatedTime = time.format(formatter);

        //Formats the scope of the punishment
        String punishmentScope = switch(p.getScope()){
            case PunishmentScopes.MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case PunishmentScopes.DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case PunishmentScopes.GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
        };

        //Setting the lore
        List<String> punishmentLore = new ArrayList<>();
        punishmentLore.add(ChatColor.translateAlternateColorCodes('&', "&8Issued at: &l"+formatedTime));
        punishmentLore.add(" ");
        punishmentLore.add(ChatColor.translateAlternateColorCodes('&', "&aScope: "+punishmentScope));
        punishmentLore.add(ChatColor.translateAlternateColorCodes('&', "&aStaff: &e&l"+p.getStaff()));
        punishmentLore.add(ChatColor.translateAlternateColorCodes('&', "&aReason: &e"+p.getReason()));

        piMeta.setLore(punishmentLore);
        punishmentItem.setItemMeta(piMeta);
        return punishmentItem;
    }
    private boolean playerHasOnlyWarns(OfflinePlayer targetPlayer) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        try(PreparedStatement ps = dbConnection.prepareStatement("SELECT type FROM punishments WHERE uuid = ? AND active = 1")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                String type = rs.getString("type");
                if(!type.contains("WARN")) return false;
            }
        }

        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) throws SQLException {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().contains("Remove a Punishment")) return;
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null) return;
        Material clickedMaterial = clickedItem.getType();

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;

        OfflinePlayer targetPlayer = plugin.getPlayerHeadsGUIs().getClickedPlayer();
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        //If the player clicks on return button
        if(clickedMaterial.equals(Material.SPECTRAL_ARROW)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getAddRemovePunishGUI().showGui(player);
            return;
        }

        //If the player clicks on a perm ban
        if(clickedMaterial.equals(Material.NETHERITE_AXE)){
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            plugin.getDatabaseManager().removePunishment(PunishmentType.PERM_BAN, targetPlayer.getUniqueId());

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aPunishment &e&lPERMANENT BAN &aremoved from player &e&l"+targetPlayer.getName()+"&a!"));
            return;
        }

        //If the player clicks on a temp ban
        if(clickedMaterial.equals(Material.IRON_AXE)){
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            plugin.getDatabaseManager().removePunishment(PunishmentType.TEMP_BAN, targetPlayer.getUniqueId());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aPunishment &e&lTEMPORARY BAN &aremoved from player &e&l"+targetPlayer.getName()+"&a!"));
            return;
        }


        //If the player clicks on a perm mute
        if(clickedMaterial.equals(Material.SOUL_LANTERN)){
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            plugin.getDatabaseManager().removePunishment(PunishmentType.PERM_MUTE, targetPlayer.getUniqueId());

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aPunishment &e&lPERMANENT MUTE &aremoved from player &e&l"+targetPlayer.getName()+"&a!"));
            return;
        }

        //If the player clicks on a temp mute
        if(clickedMaterial.equals(Material.LANTERN)){
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            plugin.getDatabaseManager().removePunishment(PunishmentType.TEMP_MUTE, targetPlayer.getUniqueId());

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aPunishment &e&lTEMPORARY MUTE &aremoved from player &e&l"+targetPlayer.getName()+"&a!"));
        }
    }
}
