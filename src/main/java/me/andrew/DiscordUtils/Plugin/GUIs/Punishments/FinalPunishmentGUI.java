//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FinalPunishmentGUI implements Listener {
    private final DiscordUtils plugin;
    private long expiredAt;

    public FinalPunishmentGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player){
        int invSize = 54;
        String title = "Approve Punishment";
        Inventory gui = Bukkit.createInventory(player, invSize, title);

        AddingState state = plugin.getPunishmentsAddingStates().get(player.getUniqueId());

        //Return button
        ItemStack returnButton = createButton(Material.SPECTRAL_ARROW, ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"), null);
        gui.setItem(30, returnButton);

        //Approve Punishment button
        String clickedPlayerName = Bukkit.getOfflinePlayer(state.targetUUID).getName();
        ItemStack approveButton = createButton(Material.GREEN_CONCRETE, ChatColor.translateAlternateColorCodes('&', "&a&lAPPROVE PUNISHMENT FOR &e"+clickedPlayerName), null);
        gui.setItem(32, approveButton);

        //Punishment Item
        List<String> punishmentItemLore = new ArrayList<>();
        String staffName = player.getName();
        String reason = state.reason;
        String scopeString = plugin.getChoosePunishScopeGUI().getStringScope(player);
        PunishmentType punishmentType = state.type;

        //Formatting created time
        long created_at = System.currentTimeMillis();
        Instant createdInstant = Instant.ofEpochMilli(created_at);
        LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String issuedAt = time.format(formatter);

        punishmentItemLore.add(ChatColor.translateAlternateColorCodes('&', "&8Issued at &l"+issuedAt));
        punishmentItemLore.add(" ");
        punishmentItemLore.add(ChatColor.translateAlternateColorCodes('&', "&aScope: "+ scopeString));
        punishmentItemLore.add(ChatColor.translateAlternateColorCodes('&', "&aStaff: &e"+ staffName));
        punishmentItemLore.add(ChatColor.translateAlternateColorCodes('&', "&aReason: &e"+reason));

        //Adds the expiration date if the punishment is temporary and not a warning
        if(punishmentType.toString().contains("TEMP") && !punishmentType.toString().contains("WARN")){
            long duration = state.duration;
            expiredAt = created_at +duration;
            Instant expireInstant = Instant.ofEpochMilli(expiredAt);
            LocalDateTime time2 = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            String expiresAt = time2.format(formatter2);

            punishmentItemLore.add(" ");
            punishmentItemLore.add(ChatColor.translateAlternateColorCodes('&', "&8Expires at &l"+expiresAt));
            punishmentItemLore.add(" ");
        }

        ItemStack punishmentItem = createButton(Material.OAK_SIGN, getPunishmentString(state.type), punishmentItemLore);
        gui.setItem(22, punishmentItem);

        player.openInventory(gui);
    }

    private ItemStack createButton(Material mat, String displayName, List<String> lore){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if(displayName != null) meta.setDisplayName(displayName);
        if(lore != null) meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
    private String getPunishmentString(PunishmentType type){
        return switch(type){
            case PunishmentType.KICK -> ChatColor.translateAlternateColorCodes('&', "&e&lKICK");
            case PunishmentType.PERM_BAN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT BAN");
            case PunishmentType.PERM_BAN_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT BAN WARN");
            case PunishmentType.PERM_MUTE -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT MUTE");
            case PunishmentType.PERM_MUTE_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT MUTE WARN");
            case PunishmentType.TEMP_BAN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY BAN");
            case PunishmentType.TEMP_BAN_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY BAN WARN");
            case PunishmentType.TEMP_MUTE -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY MUTE");
            case PunishmentType.TEMP_MUTE_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY MUTE WARN");
        };
    }
    private void insertPunishment(Player staff, AddingState state, long expired_at){
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String chatPrefix = plugin.getConfig().getString("chat-prefix");
        String playerName = staff.getName();
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);

        try(PreparedStatement ps = dbConnection.prepareStatement("""
                INSERT INTO punishments (id, uuid, type, scope, reason, staff, created_at, expire_at, active, removed, removed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)){
            ps.setString(1, state.ID);
            ps.setString(2, targetPlayer.getUniqueId().toString());
            ps.setString(3, state.type.name());
            ps.setString(4, state.scope.name());
            ps.setString(5, state.reason);
            ps.setString(6, playerName);
            ps.setLong(7, System.currentTimeMillis());

            //Handling various cases
            if(state.type.equals(PunishmentType.KICK)){
                ps.setLong(8, 0);
                ps.setBoolean(9, false);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            else if(state.type.toString().contains("WARN")){
                ps.setLong(8, 0);
                ps.setBoolean(9, true);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            else if(state.type.name().contains("TEMP")){
                ps.setLong(8, expired_at);
                ps.setBoolean(9, true);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            else if(state.type.toString().contains("PERM")){
                ps.setLong(8, 0);
                ps.setBoolean(9, true);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            ps.executeUpdate();
            plugin.getPunishmentsAddingStates().remove(staff.getUniqueId());
        } catch(Exception e){
            staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&',  chatPrefix + " &cThere was a problem applying the punishment. See the CMD of the server for more info."));
            Bukkit.getLogger().warning("[DISCORDUTILS] "+e.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) throws SQLException {
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!e.getView().getTitle().equals("Approve Punishment")) return;
        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null) return;
        Material clickedMaterial = clickedItem.getType();

        ItemMeta clickedMeta =  clickedItem.getItemMeta();
        if(clickedMeta == null) return;

        AddingState state = plugin.getPunishmentsAddingStates().get(player.getUniqueId());

        //If the staff clicks on return button
        if(clickedMaterial.equals(Material.SPECTRAL_ARROW)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            state.scope = null;
            state.lastInteraction = System.currentTimeMillis();
            plugin.getChoosePunishScopeGUI().showGui(player);
            return;
        }

        //If the staff clicks on Approve Punishment button
        if(clickedMaterial.equals(Material.GREEN_CONCRETE)){
            player.closeInventory();

            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);
            String clickedPlayerName = targetPlayer.getName();
            String chatPrefix = plugin.getConfig().getString("chat-prefix");
            PunishmentScopes scope =  state.scope;
            PunishmentType punishmentType = state.type;
            String reason = state.reason;

            //If the punishment is a kick
            boolean toggleMessages = plugin.getConfig().getBoolean("player-punishments-messages.toggle");
            if(punishmentType.equals(PunishmentType.KICK) && (scope.equals(PunishmentScopes.GLOBAL) || scope.equals(PunishmentScopes.MINECRAFT))){
                if(targetPlayer.isOnline()){
                    String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.kick-message"));
                    ((Player) targetPlayer).kickPlayer(message
                            .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                            .replace("%reason%", reason)
                    );
                    insertPunishment(player, state, 0);
                }
                else{
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+clickedPlayerName+" &cis offline at the moment!"));
                    return;
                }
            }

            //If the punishment is a perm ban warn
            if(punishmentType.equals(PunishmentType.PERM_BAN_WARN) && (scope.equals(PunishmentScopes.GLOBAL) || scope.equals(PunishmentScopes.MINECRAFT))){
                insertPunishment(player, state, 0);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getPunishmentString(state.type)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);

                //Check if the player reached the designated amount of warns
                if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, state.type, state.scope)){
                    if(targetPlayer.isOnline()){
                        String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.perm-ban-message"));
                        ((Player) targetPlayer).kickPlayer(message
                                .replace("%scope%",  plugin.getChoosePunishScopeGUI().getStringScope(player))
                                .replace("%reason%", reason)
                        );
                    }

                    plugin.getDatabaseManager().expireAllWarns(targetPlayer, PunishmentType.PERM_BAN_WARN, state.scope);
                    insertPunishment(player, state, 0);
                }
                else{
                    //Sends the target player a message if he is online
                    if (targetPlayer.isOnline()) {
                        if (toggleMessages) {
                            List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-ban-warn-message");
                            for (String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                                    .replace("%scope%",  plugin.getChoosePunishScopeGUI().getStringScope(player))
                                    .replace("%reason%", reason)
                            ));
                        }
                    }
                }
            }

            //If punishment is a perm ban
            if(punishmentType.equals(PunishmentType.PERM_BAN) && (scope.equals(PunishmentScopes.GLOBAL) || scope.equals(PunishmentScopes.MINECRAFT))){
                //Checks if the player is online, it kicks him with a message
                if(targetPlayer.isOnline()){
                    String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.perm-ban-message"));
                    ((Player) targetPlayer).kickPlayer(message
                            .replace("%scope%",  plugin.getChoosePunishScopeGUI().getStringScope(player))
                            .replace("%reason%", reason)
                            .replace("%id%", state.ID)
                    );
                }

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPunishment "+getPunishmentString(state.type)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                insertPunishment(player, state, 0);
            }

            //If punishment is a temp ban warn
            if(punishmentType.equals(PunishmentType.TEMP_BAN_WARN) && (scope.equals(PunishmentScopes.GLOBAL) || scope.equals(PunishmentScopes.MINECRAFT))){
                insertPunishment(player, state, 0);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getPunishmentString(state.type)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);

                //Check if the player has the designated amount of warns
                if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.TEMP_BAN_WARN, state.scope)){
                    long duration = state.duration;
                    long createdAt = System.currentTimeMillis();
                    long expiresAt = createdAt + duration;

                    insertPunishment(player, state, expiresAt);
                    plugin.getDatabaseManager().expireAllWarns(targetPlayer, PunishmentType.TEMP_BAN_WARN,  state.scope);

                    if(targetPlayer.isOnline()){
                        Instant expireInstant = Instant.ofEpochMilli(createdAt + duration);
                        LocalDateTime time = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                        String expiresAtString = time.format(formatter);

                        String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.temp-ban-message"));
                        ((Player) targetPlayer).kickPlayer(message
                                .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                                .replace("%reason%", reason)
                                .replace("%expiration_time%", expiresAtString)
                                .replace("%time_left%", plugin.formatTime(duration))
                                .replace("%id%", state.ID)
                        );
                    }
                }
                else{
                    //Sends the target player a message if he is online
                    if (targetPlayer.isOnline()) {
                        if (toggleMessages){
                            List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-ban-warn-message");
                            for (String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                                    .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                                    .replace("%reason%", reason)
                            ));
                        }
                    }
                }
            }

            //If punishment is a temp ban
            if(punishmentType.equals(PunishmentType.TEMP_BAN) && (scope.equals(PunishmentScopes.GLOBAL) || scope.equals(PunishmentScopes.MINECRAFT))) {
                if(targetPlayer.isOnline()){
                    String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.temp-ban-message"));
                    long duration = state.duration;

                    Instant expireInstant = Instant.ofEpochMilli(expiredAt);
                    LocalDateTime time = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                    String expiresAt = time.format(formatter);

                    ((Player) targetPlayer).kickPlayer(message
                            .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                            .replace("%reason%", reason)
                            .replace("%expiration_time%", expiresAt)
                            .replace("%time_left%", plugin.formatTime(duration))
                            .replace("%id%", state.ID)
                    );
                }

                insertPunishment(player, state, expiredAt);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPunishment "+getPunishmentString(punishmentType)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
            }

            //If punishment is a perm mute
            if(punishmentType == PunishmentType.PERM_MUTE && (scope.equals(PunishmentScopes.MINECRAFT) || scope.equals(PunishmentScopes.GLOBAL))){
                //Sends the player a chat message if he is online
                boolean toggleMessage = plugin.getConfig().getBoolean("player-punishments-messages.toggle");
                if(targetPlayer.isOnline() && toggleMessage){
                    List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-mute-message");
                    for(String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                            .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                            .replace("%reason%", reason)
                            .replace("%id%", state.ID)
                    ));
                }

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPunishment &e&l"+getPunishmentString(punishmentType)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                insertPunishment(player, state, 0);
            }

            //If punishment is a perm mute warning
            if(punishmentType == PunishmentType.PERM_MUTE_WARN && (scope == PunishmentScopes.MINECRAFT || scope == PunishmentScopes.GLOBAL)){
                insertPunishment(player, state, 0);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getPunishmentString(state.type)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);

                //Check if the player reached the designated amount of warns
                if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.PERM_MUTE_WARN, state.scope)){
                    if(targetPlayer.isOnline()){
                        List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-mute-message");
                        for(String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                                .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                                .replace("%reason%", reason)
                        ));
                    }

                    plugin.getDatabaseManager().expireAllWarns(targetPlayer, PunishmentType.PERM_MUTE_WARN,  state.scope);
                    insertPunishment(player, state, 0);
                }
                else{
                    //Sends the target player a message if he is online
                    if (targetPlayer.isOnline()) {
                        if (toggleMessages) {
                            List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-mute-warn-message");
                            for (String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                                    .replace("%scope%",  plugin.getChoosePunishScopeGUI().getStringScope(player))
                                    .replace("%reason%", reason)
                            ));
                        }
                    }
                }
            }

            //If punishment is a temp mute
            if(punishmentType == PunishmentType.TEMP_MUTE && (scope == PunishmentScopes.MINECRAFT || scope == PunishmentScopes.GLOBAL)){
                //Sends the target player a chat message if he is online
                boolean toggleMessage = plugin.getConfig().getBoolean("player-punishments-messages.toggle");
                if(targetPlayer.isOnline() && toggleMessage){
                    List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-message");

                    //Getting the duration and the expiration date
                    long duration = state.duration;

                    Instant expireInstant = Instant.ofEpochMilli(expiredAt);
                    LocalDateTime time = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                    String expiresAt = time.format(formatter);

                    for(String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                            .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                            .replace("%reason%", reason)
                            .replace("%expiration_time%", expiresAt)
                            .replace("%time_left%", plugin.formatTime(duration))
                            .replace("%id%", state.ID)
                    ));
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aPunishment &e&l"+getPunishmentString(punishmentType)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                insertPunishment(player, state, expiredAt);
            }

            //If punishment is a temp mute warning
            if(punishmentType == PunishmentType.TEMP_MUTE_WARN && (scope == PunishmentScopes.MINECRAFT || scope == PunishmentScopes.GLOBAL)){
                insertPunishment(player, state, 0);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getPunishmentString(state.type)+" &aapplied for player &e"+clickedPlayerName+"&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);

                //Check if the player has the designated amount of warns
                if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.TEMP_MUTE_WARN, state.scope)){
                    long duration = state.duration;
                    long createdAt = System.currentTimeMillis();
                    long expiresAt = createdAt + duration;

                    insertPunishment(player, state, expiresAt);
                    plugin.getDatabaseManager().expireAllWarns(targetPlayer, PunishmentType.TEMP_MUTE_WARN,  scope);

                    if(targetPlayer.isOnline()){
                        Instant expireInstant = Instant.ofEpochMilli(createdAt + duration);
                        LocalDateTime time = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                        String expiresAtString = time.format(formatter);

                        List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-message");
                        for(String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                                .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                                .replace("%reason%", reason)
                                .replace("%expiration_time%", expiresAtString)
                                .replace("%time_left%", plugin.formatTime(duration))
                        ));
                    }
                }
                else{
                    //Sends the target player a message if he is online
                    if (targetPlayer.isOnline()) {
                        if (toggleMessages){
                            List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-warn-message");
                            for (String messageLine : message) ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                                    .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(player))
                                    .replace("%reason%", reason)
                            ));
                        }
                    }
                }
            }
        }
    }
}
