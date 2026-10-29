//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.DiscordBot.InsertLog;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentContext;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FinalPunishmentGUI implements Listener {
    private final DiscordUtils plugin;

    public FinalPunishmentGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player){
        int invSize = 54;
        Component title = Component.text("Approve Punishment");
        Inventory gui = Bukkit.createInventory(player, invSize, title);

        AddingState state = plugin.getPunishmentsAddingStates().get(player.getUniqueId());

        //Return button
        ItemStack returnButton = createButton(Material.SPECTRAL_ARROW, LegacyComponentSerializer.legacyAmpersand().deserialize("&c&lRETURN"), null);
        gui.setItem(30, returnButton);

        //Approve Punishment button
        String clickedPlayerName = Bukkit.getOfflinePlayer(state.targetUUID).getName();
        ItemStack approveButton = createButton(Material.GREEN_CONCRETE, LegacyComponentSerializer.legacyAmpersand().deserialize("&a&lAPPROVE PUNISHMENT FOR &e"+clickedPlayerName), null);
        gui.setItem(32, approveButton);

        //Punishment Item
        List<Component> punishmentItemLore = new ArrayList<>();
        String staffName = player.getName();
        String reason = state.reason;
        String scopeString = getPunishmentColoredScope(state.scope);
        PunishmentType punishmentType = state.type;

        //Formatting created time
        long created_at = System.currentTimeMillis();
        Instant createdInstant = Instant.ofEpochMilli(created_at);
        LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String issuedAt = time.format(formatter);

        punishmentItemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Issued at &l"+issuedAt));
        punishmentItemLore.add(Component.text(" "));
        punishmentItemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aScope: "+ scopeString));
        punishmentItemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aStaff: &e"+ staffName));
        punishmentItemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aReason: &e"+reason));

        //Adds the expiration date if the punishment is temporary
        if(!punishmentType.isPermanent()){
            long duration = state.duration;
            long expiredAt = created_at + duration;
            Instant expireInstant = Instant.ofEpochMilli(expiredAt);
            LocalDateTime time2 = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            String expiresAt = time2.format(formatter2);

            punishmentItemLore.add(Component.text(" "));
            punishmentItemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Expires at &l"+expiresAt));
            punishmentItemLore.add(Component.text(" "));
        }

        ItemStack punishmentItem = createButton(Material.OAK_SIGN, Component.text("&e&l"+state.type.getName()), punishmentItemLore);
        gui.setItem(22, punishmentItem);

        player.openInventory(gui);
    }

    private ItemStack createButton(Material mat, Component displayName, List<Component> lore){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if(displayName != null) meta.displayName(displayName);
        if(lore != null) meta.lore(lore);

        item.setItemMeta(meta);
        return item;
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
        if(!e.getView().title().equals(Component.text("Approve Punishment"))) return;
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
            if ((scope == PunishmentScopes.DISCORD || scope == PunishmentScopes.GLOBAL) && !plugin.getPlayerVerificationCache().isPlayerVerifiedUUID(state.targetUUID)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &cPlayer &e" + clickedPlayerName + " &cis not verified on the discord server!"));
                return;
            }

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
            }

            //Creating the context for the punishment
            PunishmentContext ctx = new PunishmentContext(plugin, player, state);

            //Applying the punishment
            scope.applyPunishment(ctx, punishmentType);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix + " &e&l"+ punishmentType.getName() + " &awith scope " + getPunishmentColoredScope(state.scope) + " &aapplied for " + clickedPlayerName + "&a!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        }

        //Inserting the log (if they are toggled)
        if(plugin.botFile().getConfig().getBoolean("use-logs", false)) new InsertLog(plugin, plugin.getDiscordBot(), state);

        //Removing the staff from the adding state map
        plugin.getPunishmentsAddingStates().remove(player.getUniqueId());
    }
}
