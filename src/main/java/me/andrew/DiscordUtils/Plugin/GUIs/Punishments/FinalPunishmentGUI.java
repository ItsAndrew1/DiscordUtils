//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.DiscordBot.InsertLog;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentContext;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.security.SecureRandom;
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
import java.util.concurrent.TimeUnit;

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

    private String getPunishmentColoredScope(PunishmentScopes scope){
        return switch(scope){
            case MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
        };
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
        if(clickedMaterial.equals(Material.GREEN_CONCRETE)) {
            player.closeInventory();

            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);
            String clickedPlayerName = targetPlayer.getName();
            String chatPrefix = plugin.getConfig().getString("chat-prefix");
            PunishmentScopes scope = state.scope;
            PunishmentType punishmentType = state.type;

            //Checking if the target player is verified (if the scope is DISCORD or GLOBAL)
            if ((scope == PunishmentScopes.DISCORD || scope == PunishmentScopes.GLOBAL) && !plugin.getDatabaseManager().isVerified(targetPlayer.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &cPlayer &e" + clickedPlayerName + " &cis not verified on the discord server!"));
                return;
            }

            //Creating the context for the punishment
            PunishmentContext ctx = new PunishmentContext(plugin, player, state);

            //If the punishment is a kick
            if (punishmentType.equals(PunishmentType.KICK)) {
                //Checking if the target player is online or not
                if (!targetPlayer.isOnline() && (scope == PunishmentScopes.MINECRAFT || scope == PunishmentScopes.GLOBAL)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    if (scope == PunishmentScopes.MINECRAFT)
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &cPlayer &e" + clickedPlayerName + " &cis not online!"));
                    if (scope == PunishmentScopes.GLOBAL)
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &cPlayer &e" + clickedPlayerName + " &cis not online! You may use &9&lDISCORD &cscope instead."));
                    return;
                }

                //Applying the punishment
                scope.applyPunishment(ctx, PunishmentType.KICK);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.KICK) + " &awith scope, " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
            }

            //If the punishment is a perm ban warn
            if (punishmentType.equals(PunishmentType.PERM_BAN_WARN)) {
                scope.applyPunishment(ctx, PunishmentType.PERM_BAN_WARN);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " " + getPunishmentString(PunishmentType.PERM_BAN_WARN) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a perm ban
            if (punishmentType.equals(PunishmentType.PERM_BAN)) {
                scope.applyPunishment(ctx, PunishmentType.PERM_BAN);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.PERM_BAN) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a temp ban warn
            if (punishmentType.equals(PunishmentType.TEMP_BAN_WARN)) {
                scope.applyPunishment(ctx, PunishmentType.TEMP_BAN_WARN);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.TEMP_BAN_WARN) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a temp ban
            if (punishmentType.equals(PunishmentType.TEMP_BAN)) {
                scope.applyPunishment(ctx, PunishmentType.TEMP_BAN);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.TEMP_BAN) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a perm mute
            if (punishmentType == PunishmentType.PERM_MUTE) {
                scope.applyPunishment(ctx, PunishmentType.PERM_MUTE);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.PERM_MUTE) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a perm mute warning
            if (punishmentType == PunishmentType.PERM_MUTE_WARN) {
                scope.applyPunishment(ctx, PunishmentType.PERM_MUTE_WARN);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.PERM_MUTE_WARN) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a temp mute
            if (punishmentType == PunishmentType.TEMP_MUTE) {
                scope.applyPunishment(ctx, PunishmentType.TEMP_MUTE);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.TEMP_MUTE) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }

            //If punishment is a temp mute warning
            if (punishmentType == PunishmentType.TEMP_MUTE_WARN) {
                scope.applyPunishment(ctx, PunishmentType.TEMP_MUTE_WARN);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &aPunishment " + getPunishmentString(PunishmentType.TEMP_MUTE_WARN) + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for player &e" + clickedPlayerName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }
        }

        //Inserting the log (if they are toggled)
        if(plugin.botFile().getConfig().getBoolean("use-logs")) new InsertLog(plugin, plugin.getDiscordBot(), state);

        //Removing the staff from the adding state map
        plugin.getPunishmentsAddingStates().remove(player.getUniqueId());
    }
}
