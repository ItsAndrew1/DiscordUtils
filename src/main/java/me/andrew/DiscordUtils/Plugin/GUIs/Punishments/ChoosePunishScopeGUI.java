//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChoosePunishScopeGUI implements Listener {
    private final DiscordUtils plugin;

    public ChoosePunishScopeGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player){
        int invSize = 54;
        Component title = Component.text("Choose Punishment Scope");
        Inventory gui = Bukkit.createInventory(player, invSize, title);

        //Return button
        ItemStack returnButton = createButton(Material.SPECTRAL_ARROW, LegacyComponentSerializer.legacySection().deserialize("&c&lRETURN"), null, null);
        gui.setItem(40, returnButton);

        //Discord Scope button
        List<Component> discordButtonLore = new ArrayList<>();
        discordButtonLore.add(Component.text(" "));
        discordButtonLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&eThe punishment will take place only in the &ldiscord server&e."));
        ItemStack discordButton = createButton(Material.PLAYER_HEAD, LegacyComponentSerializer.legacyAmpersand().deserialize("&9&lDISCORD SCOPE"), discordButtonLore ,"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19");
        gui.setItem(20,  discordButton);

        //Minecraft Scope button
        List<Component> mcButtonLore = new ArrayList<>();
        mcButtonLore.add(Component.text(" "));
        mcButtonLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&eThe punishment will take place only in the &lminecraft server&e."));
        ItemStack minecraftButton = createButton(Material.PLAYER_HEAD, LegacyComponentSerializer.legacyAmpersand().deserialize("&2&LMINECRAFT SCOPE"), mcButtonLore,"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjFiNjM1YmM5MmRkMjAwMTFkNmU3ZDAxMjU3ZTZlYTczN2I0OWZiN2RlOTQxZDNiYmQxODc3MTA0ZmM1ZWIxZSJ9fX0=");
        gui.setItem(22, minecraftButton);

        //Global Scope button
        List<Component> globalButtonLore = new ArrayList<>();
        globalButtonLore.add(Component.text(" "));
        globalButtonLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&eThe punishment will take place in both &lminecraft &eand &ldiscord server&e."));
        ItemStack globalButton = createButton(Material.PLAYER_HEAD, LegacyComponentSerializer.legacyAmpersand().deserialize("&6&lGLOBAL SCOPE"), globalButtonLore, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTU3N2M0ZGUxZjUxYTcwNzIyMDIzZTg1NmI1NDNjZDU3MGYxZDBlZTZiOWQxNjdiNTkwMjhjZTFiYzkyZTQ1OCJ9fX0=");
        gui.setItem(24, globalButton);

        player.openInventory(gui);
    }

    private ItemStack createButton(Material mat, Component displayName, List<Component> lore, String customHeadValue){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        //Setting custom head
        if(customHeadValue != null){
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            skullMeta.displayName(displayName);
            if(lore != null) skullMeta.lore(lore);
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", customHeadValue));
            skullMeta.setPlayerProfile(profile);

            item.setItemMeta(skullMeta);
            return item;
        }

        meta.displayName(displayName);
        if(lore != null) meta.lore(lore);
        item.setItemMeta(meta);

        item.setItemMeta(meta);
        return item;
    }

    private void checkForWarns(Player staff, OfflinePlayer targetPlayer, AddingState state) {
        PunishmentType type = state.type;
        long currentNrOfWarns = getNrOfWarns(targetPlayer.getUniqueId(), type, state.scope);
        long designatedNrWarns = plugin.getConfig().getLong("warns-amount");

        if(currentNrOfWarns == designatedNrWarns - 1 && (type == PunishmentType.TEMP_BAN_WARN || type == PunishmentType.TEMP_MUTE_WARN)){
            staff.closeInventory();
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the duration for the temporary ban. Type &c&lcancel &ato exit this."));
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aExample: &e2d 5h 10m 35s"));
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&ld &f-> &adays | &f&lh &f-> &ahours | &f&lm &f-> &aminutes | &f&ls-> &aseconds"));

            plugin.waitForPlayerInput(staff, input -> {
                if(input.equals(Component.text("cancel"))){
                    staff.playSound(staff.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    state.scope = null;
                    showGui(staff);
                    return;
                }
                long durationFromWarnings = parseCooldown(PlainTextComponentSerializer.plainText().serialize(input));

                if(durationFromWarnings == 0){
                    staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid duration."));
                    //Reopens the gui after 1/2 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            showGui(staff);
                            state.scope = null;
                        }
                    }.runTaskLater(plugin, 10L);
                } else{
                    state.duration = durationFromWarnings;
                    state.lastInteraction = System.currentTimeMillis();
                    plugin.getFinalPunishmentGUI().showGui(staff);
                }
            });
        } else{
            state.lastInteraction = System.currentTimeMillis();
            plugin.getFinalPunishmentGUI().showGui(staff);
        }
    }
    private long parseCooldown(String cooldown){
        long millis = 0;
        Matcher m = Pattern.compile("(\\d+)([dhms])").matcher(cooldown.toLowerCase());

        while(m.find()){
            int value = Integer.parseInt(m.group(1));
            switch(m.group(2)){
                case "d" -> millis += value*86400000L;
                case "h" -> millis += value*3600000L;
                case "m" -> millis += value*60000L;
                case "s" -> millis += value*1000L;
            }
        }
        return millis;
    }

    private boolean isBotConfigured(){
        String guildId = plugin.botFile().getConfig().getString("guild-id");
        String botToken = plugin.botFile().getConfig().getString("bot-token");

        return guildId != null && botToken != null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!e.getView().title().equals(Component.text("Choose Punishment Scope"))) return;
        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null) return;
        Material clickedMaterial = clickedItem.getType();

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;

        AddingState state = plugin.getPunishmentsAddingStates().get(player.getUniqueId());

        //If the player clicks on return button
        if(clickedMaterial.equals(Material.SPECTRAL_ARROW)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            if(state.scope != null) state.scope = null; //Resets the saved scope

            state.lastInteraction = System.currentTimeMillis();
            plugin.getChoosePunishTypeGUI().showGui(player);
            return;
        }

        OfflinePlayer targetPlayer =  plugin.getPlayerHeadsGUIs().getClickedPlayer();

        //If the player clicks on discord scope button
        if(clickedMeta.displayName().contains(Component.text("&9&lDISCORD"))){
            //Checking if the bot is configured
            if(!isBotConfigured()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThe bot is not configured. Please configure it in the &ebotconfig.yml&c file."));
                return;
            }

            state.scope = PunishmentScopes.DISCORD;
        }

        //If the player clicks on minecraft scope button
        if(clickedMeta.displayName().contains(Component.text("&2&lMINECRAFT"))) state.scope = PunishmentScopes.MINECRAFT;

        //If the player clicks on global scope button
        if(clickedMeta.displayName().contains(Component.text("&6&lGLOBAL"))) {
            if(!isBotConfigured()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThe bot is not configured. Please configure it in the &ebotconfig.yml&c file."));
                return;
            }

            state.scope = PunishmentScopes.GLOBAL;
        }

        //Checking if the target player already has a punishment of that scope.
        if(state.scope.playerAlreadyHasPunishment(state.type, targetPlayer, player, plugin)) return;

        checkForWarns(player, targetPlayer, state);
    }

    private long getNrOfWarns(UUID uuid, PunishmentType warnType, PunishmentScopes warnScope){
        List<Punishment> punishments = plugin.getPlayerPunishmentDataCache().getMainCache().get(uuid);
        if(punishments == null) return 0;

        return punishments.stream().filter(p -> p.punishmentType().equals(warnType) && p.scope().equals(warnScope) && p.active()).count();
    }
}
