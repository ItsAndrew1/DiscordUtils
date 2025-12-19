//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.GUIs;

import me.andrew.DiscordUtils.DiscordUtils;
import org.bukkit.*;
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

public class AppearanceChoiceGUI implements Listener {
    private final DiscordUtils plugin;
    private boolean chatMessage, book, applyChanges;
    private String guiTitle;
    private String choice;

    public AppearanceChoiceGUI(DiscordUtils plugin) {
        this.plugin = plugin;
        chatMessage = false;
        book = false;
        applyChanges = false;
    }

    public void showGUI(Player player){
        guiTitle = "Choose appearance type";
        Inventory GUI = Bukkit.createInventory(null, 54, guiTitle);

        //Return button
        ItemStack returnButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta returnButtonMeta = returnButton.getItemMeta();
        if(!applyChanges){
            returnButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
            returnButton.setItemMeta(returnButtonMeta);
            GUI.setItem(40, returnButton);
        }

        //Apply Changes button
        if(applyChanges){
            //Sets the return button one slot to the right
            GUI.setItem(41, returnButton);

            ItemStack applyChangesButton = new ItemStack(Material.GREEN_CONCRETE);
            ItemMeta applyChangesButtonMeta = applyChangesButton.getItemMeta();
            applyChangesButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lAPPLY CHANGES"));
            applyChangesButton.setItemMeta(applyChangesButtonMeta);
            GUI.setItem(39, applyChangesButton);
        }

        //Book choice button
        ItemStack bookButton = new ItemStack(Material.BOOK);
        ItemMeta bookButtonMeta = bookButton.getItemMeta();
        if(book){ //Adds the enchant glint if the boolean is true
            bookButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            bookButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        bookButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lBOOK APPEARANCE"));
        bookButton.setItemMeta(bookButtonMeta);
        GUI.setItem(20, bookButton);

        //Chat choice button
        ItemStack chatButton = new ItemStack(Material.PAPER);
        ItemMeta chatButtonMeta = chatButton.getItemMeta();
        if(chatMessage){ //Add the enchant glint if the boolean is true
            chatButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            chatButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        chatButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lCHAT APPEARANCE"));
        chatButton.setItemMeta(chatButtonMeta);
        GUI.setItem(24, chatButton);

        player.openInventory(GUI);
    }

    private void setBooleansToFalse(){
        book = false;
        chatMessage = false;
        applyChanges = false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().equals(guiTitle)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null) return;

        ItemStack clickedMeta =  event.getCurrentItem();
        if(clickedMeta == null) return;
        Material clickedMaterial = clickedMeta.getType();

        Sound clickSound = Registry.SOUNDS.get(NamespacedKey.minecraft("ui.button.click"));
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        //If the player clicks on return button
        Material retButton = Material.RED_CONCRETE;
        if(clickedMaterial.equals(retButton)){
            event.setCancelled(true);
            setBooleansToFalse();
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            plugin.getMainConfigGUI().showGUI(player);
        }

        //If the player clicks on book button
        Material bookButton = Material.BOOK;
        if(clickedMaterial.equals(bookButton)){
            event.setCancelled(true);
            setBooleansToFalse();
            book = true;
            applyChanges = true;
            choice = "book";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGUI(player);
        }

        //If the player clicks on chat button
        Material chatButton = Material.PAPER;
        if(clickedMaterial.equals(chatButton)){
            event.setCancelled(true);
            setBooleansToFalse();
            chatMessage = true;
            applyChanges = true;
            choice = "chat-message";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGUI(player);
        }

        //If the player clicks on apply changes button
        Material applyChangesButton = Material.GREEN_CONCRETE;
        if(clickedMaterial.equals(applyChangesButton)){
            event.setCancelled(true);
            player.closeInventory();

            plugin.getConfig().set("link-appearance-choice", choice);
            plugin.saveConfig();

            Sound savedChoice =  Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
            player.playSound(player.getLocation(), savedChoice, 1f, 1.4f);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aSaved appearance choice &l"+choice+"&a!"));
            setBooleansToFalse();
            applyChanges = false;

            //Re-opens the main config GUI after 1/2 seconds
            new BukkitRunnable(){
                @Override
                public void run() {
                    plugin.getMainConfigGUI().showGUI(player);
                }
            }.runTaskLater(plugin, 10L);
        }
    }
}
