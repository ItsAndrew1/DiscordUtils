//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.Punishment;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AddRemoveHistoryGUI implements Listener{
    private final DiscordUtils plugin;

    public AddRemoveHistoryGUI(DiscordUtils plugin){
        this.plugin = plugin;

        //Start a task which removes a user from the AddingState map after a period of time of inactivity.
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, ()->{
            long now = System.currentTimeMillis();
            int inactivityMinutes = plugin.getConfig().getInt("inactivity-minutes");
            plugin.getPunishmentsAddingStates().entrySet().removeIf(entry ->
                    now - entry.getValue().lastInteraction > TimeUnit.MINUTES.toMillis(inactivityMinutes)
            );
        }, 0L, 20L * 180); //Runs every 3 minutes
    }

    public void showGui(Player player){
        int invSize = 54;
        String invTitle = "Add/Remove Punishment";
        Inventory gui = Bukkit.createInventory(null, invSize, invTitle);
        String playerName = plugin.getPlayerHeadsGUIs().getClickedPlayer().getName();

        //Add button
        ItemStack addButton = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta addButtonSkull =  (SkullMeta) addButton.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19"));
        addButtonSkull.setPlayerProfile(profile);
        addButtonSkull.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aAdd &lpunishment &afor player &e&l"+playerName));
        addButton.setItemMeta(addButtonSkull);
        gui.setItem(20, addButton);

        //Remove button
        ItemStack removeButton = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta removeButtonSkull = (SkullMeta) removeButton.getItemMeta();
        PlayerProfile profile2 = Bukkit.createProfile(UUID.randomUUID());
        profile2.setProperty(new ProfileProperty("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU0YjhiOGQyMzYyYzg2NGUwNjIzMDE0ODdkOTRkMzI3MmE2YjU3MGFmYmY4MGMyYzViMTQ4Yzk1NDU3OWQ0NiJ9fX0="));
        removeButtonSkull.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cRemove &lpunishment &cfor player &e&l"+playerName));
        removeButton.setItemMeta(removeButtonSkull);
        gui.setItem(22, removeButton);

        //History button
        ItemStack historyButton = new ItemStack(Material.PAPER);
        ItemMeta historyButtonMeta = historyButton.getItemMeta();
        historyButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5See the &lpunishment history &5of &e&l"+playerName));
        historyButton.setItemMeta(historyButtonMeta);
        gui.setItem(24, historyButton);

        //Return button
        ItemStack returnButton = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta returnButtonMeta = returnButton.getItemMeta();
        returnButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
        returnButton.setItemMeta(returnButtonMeta);
        gui.setItem(40, returnButton);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) throws SQLException {
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!e.getView().getTitle().equalsIgnoreCase("Add/Remove Punishment")) return;
        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null) return;

        Material clickedMat =  clickedItem.getType();
        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        if(clickedItemMeta == null) return;

        //If the player clicks on return button
        if(clickedMat.equals(Material.SPECTRAL_ARROW)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getPlayerHeadsGUIs().showGui(player, 1);
        }

        //If the player clicks on history button
        if(clickedMat.equals(Material.PAPER)){
            //Checking if the player has permission to view the history of a player
            if(!player.hasPermission("discordutils.punishments.playerhistory")){
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to view the punishment history of other players!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                return;
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getPunishmentsGUI().showGui(player, 1, false);
        }

        //If the player clicks on add punishment button
        if(clickedItemMeta.getDisplayName().contains("Add")){
            //Checking if the player has permission to add
            if(!player.hasPermission("discordutils.punishments.add")){
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to add punishments!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                return;
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

            //Inserts the staff into the addingStates map
            AddingState newState = new AddingState(
                    null,
                    plugin.getPlayerHeadsGUIs().getClickedPlayer().getUniqueId(),
                    player.getName(),
                    null,
                    null,
                    null,
                    0,
                    System.currentTimeMillis()
            );
            plugin.getPunishmentsAddingStates().put(player.getUniqueId(), newState);

            plugin.getChoosePunishTypeGUI().showGui(player);
        }

        //If the player clicks on remove punishment button
        if(clickedItemMeta.getDisplayName().contains("Remove")){
            //Checking if the player has permission
            if(!player.hasPermission("discordutils.punishments.remove")){
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to remove punishments!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                return;
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the ID of the punishment you want to remove (&7E.G: &l#123456&a). Type &c&lcancel &ato return."));
            plugin.waitForPlayerInput(player, input -> removePunishment(input, player));
        }
    }

    private void removePunishment(String ID, Player staff){
        if(ID.equalsIgnoreCase("cancel")){
            showGui(staff);
            return;
        }

        String chatPrefix = plugin.getConfig().getString("chat-prefix");
        try{
            //Checking if there is a punishment with that ID
            if(!punishmentExists(ID)){
                staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThere is no punishment with ID &l"+ID+"&c!"));
                staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                //Re-opens the GUI after 1/2 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> showGui(staff), 10L);
                return;
            }

            //Getting the punishment with that ID
            Punishment targetP = getPunishment(ID);
            plugin.getDatabaseManager().removePunishment(targetP.getPunishmentType(), targetP.getUuid());

            //If the scope is discord/global, I have to unban/remove the target user's timeout
            if(targetP.getScope() == PunishmentScopes.DISCORD || targetP.getScope() == PunishmentScopes.GLOBAL){
                String targetUserID = getTargetUserID(targetP.getUuid());
                Guild dcServer = plugin.getDiscordBot().getDiscordServer();

                if(targetP.getPunishmentType() == PunishmentType.PERM_BAN || targetP.getPunishmentType() == PunishmentType.TEMP_BAN){
                    //Removes the 'banned' role (and give him the 'verified' role) from the member if he has it
                    dcServer.retrieveMemberById(targetUserID).queue(member -> {
                        long bannedRoleID = plugin.botFile().getConfig().getLong("ban-role-id");
                        Role bannedRole = dcServer.getRoleById(bannedRoleID);

                        long verifiedRoleID = plugin.botFile().getConfig().getLong("verification.verified-role-id");
                        Role verifiedRole = dcServer.getRoleById(verifiedRoleID);
                        if(member.getRoles().contains(bannedRole)){
                            dcServer.removeRoleFromMember(member, bannedRole).queue();
                            dcServer.addRoleToMember(member, verifiedRole).queue();
                        }
                    });
                }

                if(targetP.getPunishmentType() == PunishmentType.TEMP_MUTE || targetP.getPunishmentType() == PunishmentType.PERM_MUTE){
                    //Removing the timeout role
                    long timeoutRoleID = plugin.botFile().getConfig().getLong("timeout-role-id");
                    dcServer.retrieveMemberById(targetUserID).queue(member ->{
                        dcServer.removeRoleFromMember(member, dcServer.getRoleById(timeoutRoleID)).queue();

                        //Removing the timeout if it is a temporary one
                        if(targetP.getPunishmentType() == PunishmentType.TEMP_MUTE) dcServer.removeTimeout(member).queue();
                    });
                }
            }

            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aPunishment with ID &7&l"+ID+" &ahas been removed from player &e"+Bukkit.getOfflinePlayer(targetP.getUuid()).getName()+"&a!"));
            staff.playSound(staff.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        } catch (Exception e){
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cThere was a problem when attempted to remove the punishment. See the server's CMD for more info"));
            Bukkit.getLogger().warning("[DISCORDUTILS] "+e.getMessage());
        }
    }

    private boolean punishmentExists(String ID) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT 1 FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    private Punishment getPunishment(String ID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT * FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return plugin.getDatabaseManager().mapPunishment(rs);
                return null;
            }
        }
    }

    private String getTargetUserID(UUID targetUUID) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT discordId FROM playersVerification WHERE uuid = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, targetUUID.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return rs.getString("discordId");
                return null;
            }
        }
    }
}
