//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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

import java.sql.SQLException;
import java.util.UUID;

public class AddRemoveHistoryGUI implements Listener{
    private final DiscordUtils plugin;

    public AddRemoveHistoryGUI(DiscordUtils plugin){
        this.plugin = plugin;
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
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getPunishmentsGUI().showGui(player, 1);
        }

        //If the player clicks on add punishment button
        if(clickedItemMeta.getDisplayName().contains("Add")){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getChoosePunishTypeGUI().showGui(player);
        }

        //If the player clicks on remove punishment button
        if(clickedItemMeta.getDisplayName().contains("Remove")){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getRemovePunishmentsGUI().showGui(player, 1);
        }
    }
}
