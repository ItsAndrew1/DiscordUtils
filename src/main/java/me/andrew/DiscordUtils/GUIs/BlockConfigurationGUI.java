package me.andrew.DiscordUtils.GUIs;

import me.andrew.DiscordUtils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockConfigurationGUI implements Listener {
    private final DiscordUtils plugin;
    private String guiTitle;

    public BlockConfigurationGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGUI(Player player){
        guiTitle = "Configure the block";
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        //Decorations
        ItemStack decoItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = decoItem.getItemMeta();
        for(int i = 0; i<=8; i++){
            if(i==4) continue;
            meta.setDisplayName(" ");
            decoItem.setItemMeta(meta);
            gui.setItem(i, decoItem);
        }
        for(int i = 45; i<=53; i++){
            meta.setDisplayName(" ");
            decoItem.setItemMeta(meta);
            gui.setItem(i, decoItem);
        }

        //Exit Button
        ItemStack returnButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta returnButtonItemMeta = returnButton.getItemMeta();
        returnButtonItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
        returnButton.setItemMeta(returnButtonItemMeta);
        gui.setItem(40, returnButton);


    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().equals(guiTitle)) return;
        event.setCancelled(true); //Makes it impossible to get/put items

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null || clickedItem.getType().equals(Material.OAK_SIGN) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)) return;

        Material clickedMaterial = clickedItem.getType();
        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;


    }
}
