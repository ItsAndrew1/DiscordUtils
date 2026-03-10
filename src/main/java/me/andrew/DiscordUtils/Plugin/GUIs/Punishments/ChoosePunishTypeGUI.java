//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChoosePunishTypeGUI implements Listener{
    private final DiscordUtils plugin;
    private long durationMillis;

    public ChoosePunishTypeGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player){
        int invSize = 54;

        String title = "Choose Punishment Type";
        Inventory gui =  Bukkit.createInventory(player, invSize, title);

        //Return button
        ItemStack returnButton = createButton(Material.SPECTRAL_ARROW, ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"), false);
        gui.setItem(35, returnButton);

        //Permanent Ban button
        ItemStack permBanButton = createButton(Material.NETHERITE_AXE, ChatColor.translateAlternateColorCodes('&', "&5&lPERMANENT BAN"), false);
        gui.setItem(9, permBanButton);

        //Temporary Ban button
        ItemStack tempBanButton = createButton(Material.IRON_AXE, ChatColor.translateAlternateColorCodes('&', "&d&lTEMPORARY BAN"), false);
        gui.setItem(11, tempBanButton);

        //Permanent Ban Warn Button
        ItemStack permBanWarnButton = createButton(Material.NETHERITE_AXE, ChatColor.translateAlternateColorCodes('&', "&5&lPERMANENT BAN WARN"),true);
        gui.setItem(13, permBanWarnButton);

        //Temporary Ban Warn Button
        ItemStack tbwButton = createButton(Material.IRON_AXE, ChatColor.translateAlternateColorCodes('&', "&d&lTEMPORARY BAN WARN"),true);
        gui.setItem(15, tbwButton);

        //Kick Button
        ItemStack kickButton = createButton(Material.LEATHER_BOOTS, ChatColor.translateAlternateColorCodes('&', "&6&lKICK"),false);
        gui.setItem(17, kickButton);

        //Permanent Mute button
        ItemStack pmButton = createButton(Material.SOUL_LANTERN, ChatColor.translateAlternateColorCodes('&', "&9&lPERMANENT MUTE"),false);
        gui.setItem(27, pmButton);

        //Temporary Mute button
        ItemStack tmButton = createButton(Material.LANTERN, ChatColor.translateAlternateColorCodes('&', "&b&lTEMPORARY MUTE"),false);
        gui.setItem(29, tmButton);

        //Permanent Mute Warn button
        ItemStack pmwButton = createButton(Material.SOUL_LANTERN, ChatColor.translateAlternateColorCodes('&', "&9&lPERMANENT MUTE WARN"), true);
        gui.setItem(31, pmwButton);

        //Temporary Mute Warn button
        ItemStack tmwButton = createButton(Material.LANTERN, ChatColor.translateAlternateColorCodes('&', "&b&lTEMPORARY MUTE WARN"), true);
        gui.setItem(33, tmwButton);

        player.openInventory(gui);
    }

    //Helper method to create all the buttons
    private ItemStack createButton(Material mat, String displayName, boolean enchantGlint){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);

        if(enchantGlint){
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    //Method for entering the duration of the punishment
    private void enterDuration(Player player, AddingState state){
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the &lduration &afor the punishment. Type &c&lcancel &ato return."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aExample: &e2d 5h 10m 35s"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&ld &f-> &adays | &f&lh &f-> &ahours | &f&lm &f-> &aminutes | &f&ls-> &aseconds"));

        plugin.waitForPlayerInput(player, input -> {
            if(input.equalsIgnoreCase("cancel")){
                plugin.getPunishmentsAddingStates().remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                plugin.getAddRemovePunishGUI().showGui(player);
                return;
            }
            durationMillis = parseCooldown(input);

            if(durationMillis == 0){
                plugin.getPunishmentsAddingStates().remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid duration."));
                //Reopens the gui after 1/2 seconds
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getAddRemovePunishGUI().showGui(player);
                    }
                }.runTaskLater(plugin, 10L);
                return;
            }

            state.duration = durationMillis;
        });
    }

    //Method for entering the reason for the punishment
    private void enterReason(Player player, AddingState state){
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the reason for the punishment. Type &c&lcancel &ato return."));
        plugin.waitForPlayerInput(player, input -> {
            if(input.equalsIgnoreCase("cancel")){
                plugin.getPunishmentsAddingStates().remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                plugin.getAddRemovePunishGUI().showGui(player);
                return;
            }

            state.lastInteraction = System.currentTimeMillis();
            state.reason = input;
        });

        if(!state.type.isPermanent()) enterDuration(player, state);
    }

    //Method for parsing from the cooldown string to milliseconds
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

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().equalsIgnoreCase("Choose Punishment Type")) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if(item == null) return;
        Material clickedMat = item.getType();

        ItemMeta meta = item.getItemMeta();
        if(meta == null) return;

        AddingState state = plugin.getPunishmentsAddingStates().get(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        //If the player clicks on return button
        if(clickedMat.equals(Material.SPECTRAL_ARROW)){
            plugin.getPunishmentsAddingStates().remove(player.getUniqueId());
            plugin.getAddRemovePunishGUI().showGui(player);
            return;
        }

        //If the player clicks on the following buttons
        if(meta.getDisplayName().contains("PERMANENT BAN WARN")) state.type = PunishmentType.PERM_BAN_WARN;

        if(clickedMat.equals(Material.NETHERITE_AXE)) state.type = PunishmentType.PERM_BAN;

        if(meta.getDisplayName().contains("TEMPORARY BAN WARN")) state.type = PunishmentType.TEMP_BAN_WARN;

        if(clickedMat.equals(Material.IRON_AXE)) state.type = PunishmentType.TEMP_BAN;

        if(clickedMat.equals(Material.LEATHER_BOOTS)) state.type = PunishmentType.KICK;

        if(meta.getDisplayName().contains("TEMPORARY MUTE WARN")) state.type = PunishmentType.TEMP_MUTE_WARN;

        if(clickedMat.equals(Material.LANTERN)) state.type = PunishmentType.TEMP_MUTE;

        if(meta.getDisplayName().contains("PERMANENT MUTE WARN")) state.type = PunishmentType.PERM_MUTE_WARN;

        if(clickedMat.equals(Material.SOUL_LANTERN)) state.type = PunishmentType.PERM_MUTE;

        if(!state.type.hasPermission(player)){
            noPermission(player);
            return;
        }

        enterReason(player, state);
    }

    private void noPermission(Player player){
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        player.closeInventory();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou don't have permission to do this! Contact the server administrators if you think this is an issue."));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }
}
