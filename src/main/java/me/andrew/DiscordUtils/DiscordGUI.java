package me.andrew.DiscordUtils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DiscordGUI implements Listener {
    private final DiscordUtils plugin;
    private Inventory gui;

    //The discord item
    private ItemStack discordItem;

    public DiscordGUI(DiscordUtils plugin, Inventory gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void showGUI(Player player){
        gui = Bukkit.createInventory(null, plugin.getGuiSize(), plugin.getGuiTitle());

        //Displays the decorations if they are toggled
        boolean toggleDecorations = plugin.getConfig().getBoolean("discord-gui.decorations.toggle");
        if(toggleDecorations){
            //Getting the info of the item
            String itemString = plugin.getConfig().getString("discord-gui.decorations.material");
            String diDisplayName = plugin.getConfig().getString("discord-gui.decorations.display-name");
            ItemStack decoItem = new ItemStack(Material.matchMaterial(itemString.toUpperCase()));
            ItemMeta diMeta = decoItem.getItemMeta();

            for(int i = 0; i<=8; i++){
                //Skip the slot of the info item if it is toggled
                boolean toggleInfoItem = plugin.getConfig().getBoolean("discord-gui.info-item.toggle");
                if(toggleInfoItem){
                    int iiSlot = plugin.getConfig().getInt("discord-gui.info-item.slot");
                    if(i == iiSlot) continue;
                }

                diMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', diDisplayName));
                decoItem.setItemMeta(diMeta);
                gui.setItem(i, decoItem);
            }

            for(int i = 45; i<=53; i++){
                diMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', diDisplayName));
                decoItem.setItemMeta(diMeta);
                gui.setItem(i, decoItem);
            }
        }

        //Display the info item if it is toggled
        boolean toggleInfoItem = plugin.getConfig().getBoolean("discord-gui.info-item.toggle");
        if(toggleInfoItem){
            ItemStack infoItem = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("discord-gui.info-item.material").toUpperCase()));
            ItemMeta infoMeta = infoItem.getItemMeta();
            int infoItemSlot = plugin.getConfig().getInt("discord-gui.info-item.slot");

            //Setting the display name
            String iiDisplayName =  plugin.getConfig().getString("discord-gui.info-item.display-name");
            infoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', iiDisplayName));

            //Setting the lore (if there is any)
            List<String> infoItemLore = plugin.getConfig().getStringList("discord-gui.info-item.lore");
            if(infoItemLore.isEmpty()) infoMeta.setLore(Collections.emptyList());
            else{
                List<String> coloredIILore = new ArrayList<>();
                for(String loreLine : infoItemLore){
                    String coloredLine = ChatColor.translateAlternateColorCodes('&', loreLine);
                    coloredIILore.add(coloredLine);
                }
                infoMeta.setLore(coloredIILore);
            }
            infoItem.setItemMeta(infoMeta);
            gui.setItem(infoItemSlot, infoItem);
        }

        //Display the exit item if it is toggled
        boolean toggleExitItem =  plugin.getConfig().getBoolean("discord-gui.exit-item.toggle");
        if(toggleExitItem){
            ItemStack exitItem = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("discord-gui.exit-item.material").toUpperCase()));
            ItemMeta exitMeta = exitItem.getItemMeta();
            int exitItemSlot = plugin.getConfig().getInt("discord-gui.exit-item.slot");
            String eiDisplayName =  plugin.getConfig().getString("discord-gui.exit-item.display-name");

            //Setting the display name
            exitMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', eiDisplayName));

            //Setting the lore (if there is any)
            List<String> exitItemLore = plugin.getConfig().getStringList("discord-gui.exit-item.lore");
            if(exitItemLore.isEmpty()) exitMeta.setLore(Collections.emptyList());
            else{
                List<String> coloredLore =  new ArrayList<>();
                for(String loreLine : exitItemLore){
                    String coloredLine = ChatColor.translateAlternateColorCodes('&', loreLine);
                    coloredLore.add(coloredLine);
                }
                exitMeta.setLore(coloredLore);
            }
            exitItem.setItemMeta(exitMeta);
            gui.setItem(exitItemSlot, exitItem);
        }

        //Displays the 'discord item' (with a custom-head or with a material)
        boolean toggleCustomHead =  plugin.getConfig().getBoolean("discord-gui.discord-item.use-custom-head");
        boolean toggleMaterial =  plugin.getConfig().getBoolean("discord-gui.discord-item.use-normal-material");
        String diDisplayName = plugin.getConfig().getString("discord-gui.discord-item.display-name");
        int diSlot =  plugin.getConfig().getInt("discord-gui.discord-item.slot");

        if(toggleCustomHead){
            String customHeadValue = plugin.getConfig().getString("discord-gui.discord-item.custom-head");
            discordItem = getHead(customHeadValue, diDisplayName);
            gui.setItem(diSlot, discordItem);
        }
        if(toggleMaterial){
            ItemStack diMaterial = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("discord-gui.discord-item.material").toUpperCase()));
            ItemMeta diMeta = diMaterial.getItemMeta();

            //Sets the display name
            diMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', diDisplayName));

            //Sets the lore (if there is any)
            List<String> diLore =  plugin.getConfig().getStringList("discord-gui.discord-item.lore");
            if(diLore.isEmpty()) diMeta.setLore(Collections.emptyList());
            else{
                List<String> coloredLore =  new ArrayList<>();
                for(String loreLine : diLore){
                    String coloredLoreLine = ChatColor.translateAlternateColorCodes('&', loreLine);
                    coloredLore.add(coloredLoreLine);
                }
                diMeta.setLore(coloredLore);
            }

            diMaterial.setItemMeta(diMeta);
            gui.setItem(diSlot, diMaterial);
        }

        //Gets the sound from config
        Sound openGuiSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("open-discord-gui-sound").toLowerCase()));
        float ogsVolume = plugin.getConfig().getInt("odgs-volume");
        float ogsPitch = plugin.getConfig().getInt("odgs-pitch");

        player.playSound(player.getLocation(), openGuiSound, ogsVolume, ogsPitch); //Plays the sound from config
        player.openInventory(gui); //Opens the inventory
    }

    //Gets the custom head for the discord button
    private ItemStack getHead(String value, String displayName){
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();

        //Sets the display name
        headMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        //Sets the lore (if there is any)
        List<String> discordItemLore = plugin.getConfig().getStringList("discord-gui.discord-item.lore");
        if(discordItemLore.isEmpty()) headMeta.setLore(Collections.emptyList());
        else{
            List<String>  coloredLore = new ArrayList<>();
            for(String loreLine : discordItemLore){
                String coloredLoreLine = ChatColor.translateAlternateColorCodes('&', loreLine);
                coloredLore.add(coloredLoreLine);
            }
            headMeta.setLore(coloredLore);
        }

        //Sets the custom-head
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", value));
        headMeta.setPlayerProfile(profile);

        head.setItemMeta(headMeta);
        return head;
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!(e.getView().getTitle().equals(plugin.getConfig().getString("discord-gui.title")))) return;
        e.setCancelled(true); //Doesn't let the player take or put items in the GUI

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null) return;

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;


    }
}
