//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
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

public class FacingChoiceGUI implements Listener {
    private final DiscordUtils plugin;
    private boolean north, south, east, west, ne, nw, se, sw; //Booleans for the click enchant glint
    private String guiTitle;
    private boolean applyChanges;
    private String choice;

    public FacingChoiceGUI(DiscordUtils plugin) {
        this.plugin = plugin;
        north = false;
        south = false;
        east = false;
        west = false;
        ne = false;
        nw = false;
        se = false;
        sw = false;
        applyChanges = false;
    }

    public void showGui(Player player){
        guiTitle = "Choose a Facing";
        Inventory GUI = Bukkit.createInventory(null, 54, guiTitle);

        //Return button
        if(!applyChanges){
            ItemStack returnButton = new ItemStack(Material.RED_CONCRETE);
            ItemMeta returnButtonMeta = returnButton.getItemMeta();
            returnButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
            returnButton.setItemMeta(returnButtonMeta);
            GUI.setItem(40, returnButton);
        }

        //The Apply Changes button (if one of the options are selected)
        if(north || south || east || west || nw || ne || sw || se){
            ItemStack applyChangeButton = new ItemStack(Material.GREEN_CONCRETE);
            ItemMeta applyChangeButtonMeta = applyChangeButton.getItemMeta();
            applyChangeButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lAPPLY CHANGES"));
            applyChangeButton.setItemMeta(applyChangeButtonMeta);
            GUI.setItem(39, applyChangeButton);

            ItemStack returnButton = new ItemStack(Material.RED_CONCRETE);
            ItemMeta returnButtonMeta = returnButton.getItemMeta();
            returnButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
            returnButton.setItemMeta(returnButtonMeta);
            GUI.setItem(41, returnButton);
        }

        //Facing NORTH
        ItemStack northButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta northButtonMeta = northButton.getItemMeta();

        if(north){
            northButtonMeta.addEnchant(Enchantment.LURE, 1, true); //Setting a random enchant
            northButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS); //Then hide it
        }
        northButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lNORTH"));
        northButton.setItemMeta(northButtonMeta);
        GUI.setItem(10, northButton);

        //Facing WEST
        ItemStack westButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta westButtonMeta = westButton.getItemMeta();

        if(west){
            westButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            westButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        westButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lWEST"));
        westButton.setItemMeta(westButtonMeta);
        GUI.setItem(12, westButton);

        //Facing SOUTH
        ItemStack southButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta southButtonMeta = southButton.getItemMeta();

        if(south){
            southButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            southButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        southButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSOUTH"));
        southButton.setItemMeta(southButtonMeta);
        GUI.setItem(14, southButton);

        //Facing EAST
        ItemStack eastButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta eastButtonMeta = eastButton.getItemMeta();

        if(east){
            eastButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            eastButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        eastButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lEAST"));
        eastButton.setItemMeta(eastButtonMeta);
        GUI.setItem(16, eastButton);

        //Facing NORTH-WEST
        ItemStack nwButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta nwButtonMeta = nwButton.getItemMeta();

        if(nw){
            nwButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            nwButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        nwButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lNORTH-WEST"));
        nwButton.setItemMeta(nwButtonMeta);
        GUI.setItem(28, nwButton);

        //Facing NORTH-EAST
        ItemStack neButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta neButtonMeta = neButton.getItemMeta();

        if(ne){
            neButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            neButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        neButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lNORTH-EAST"));
        neButton.setItemMeta(neButtonMeta);
        GUI.setItem(30, neButton);

        //Facing SOUTH-WEST
        ItemStack swButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta swButtonMeta = swButton.getItemMeta();

        if(sw){
            swButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            swButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        swButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSOUTH-WEST"));
        swButton.setItemMeta(swButtonMeta);
        GUI.setItem(32, swButton);

        //Facing SOUTH-EAST
        ItemStack seButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta seButtonMeta = seButton.getItemMeta();

        if(se){
            seButtonMeta.addEnchant(Enchantment.LURE, 1, true);
            seButtonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        seButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSOUTH-EAST"));
        seButton.setItemMeta(seButtonMeta);
        GUI.setItem(34, seButton);

        player.openInventory(GUI);
    }

    //Helper class to set all booleans to false
    private void setAllBooleansFalse(){
        north = false;
        south = false;
        east = false;
        west = false;
        ne = false;
        nw = false;
        se = false;
        sw = false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!e.getView().getTitle().equals(guiTitle)) return;

        ItemStack clickedItem =  e.getCurrentItem();
        if(clickedItem == null) return;

        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        if(clickedItemMeta == null) return;

        //Storing the clicked display name and material
        String ciDisplayName =  clickedItemMeta.getDisplayName();
        Material clickedMaterial = clickedItem.getType();

        Sound clickSound = Registry.SOUNDS.get(NamespacedKey.minecraft("ui.button.click"));
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        //If the staff clicks on return button
        Material returnButton = Material.RED_CONCRETE;
        if(clickedMaterial.equals(returnButton)){
            e.setCancelled(true);
            setAllBooleansFalse();
            applyChanges = false;
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            plugin.getBlockConfigurationGUI().showGUI(player);
            return;
        }

        //If the staff clicks on north facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lNORTH"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            north = true;
            applyChanges = true;
            choice = "NORTH";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on west facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lWEST"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            west = true;
            applyChanges = true;
            choice = "WEST";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on south facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lSOUTH"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            south = true;
            applyChanges = true;
            choice = "SOUTH";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on east facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lEAST"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            east = true;
            applyChanges = true;
            choice = "EAST";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on north-west facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lNORTH-WEST"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            nw = true;
            applyChanges = true;
            choice = "NORTH_WEST";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on north-east facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lNORTH-EAST"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            ne = true;
            applyChanges = true;
            choice = "NORTH_EAST";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on south-west facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lSOUTH-WEST"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            sw = true;
            applyChanges = true;
            choice = "SOUTH_WEST";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on south-east facing
        if(ciDisplayName.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&b&lSOUTH-EAST"))){
            e.setCancelled(true);
            setAllBooleansFalse();
            se = true;
            applyChanges = true;
            choice = "SOUTH_EAST";
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGui(player);
        }

        //If the staff clicks on apply changes button
        Material applyChange = Material.GREEN_CONCRETE;
        if(clickedMaterial.equals(applyChange)){
            e.setCancelled(true);
            player.closeInventory();

            plugin.getConfig().set("facing", choice);
            plugin.saveConfig();

            Sound savedChoice =  Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
            player.playSound(player.getLocation(), savedChoice, 1f, 1.4f);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aSaved facing &l"+choice+" &afor the discord block!"));
            setAllBooleansFalse();
            applyChanges = false;

            //Re-opens the block config GUI after 1.2 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getBlockConfigurationGUI().showGUI(player);
                }
            }.runTaskLater(plugin, 10L);
        }
    }
}
