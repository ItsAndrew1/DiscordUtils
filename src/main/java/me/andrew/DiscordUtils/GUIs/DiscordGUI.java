//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.GUIs;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.andrew.DiscordUtils.DiscordUtils;
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

    //The discord item
    private ItemStack discordItem;

    public DiscordGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGUI(Player player){
        //Checks the gui size
        if(plugin.getGuiSize() < 9 || plugin.getGuiSize() > 54){
            error(player);
            Bukkit.getLogger().warning("[DISCORDUTILS] The value of 'discord-gui.rows' is invalid! Set a valid one and restart the server.");
            return;
        }
        Inventory gui = Bukkit.createInventory(null, plugin.getGuiSize(), plugin.getGuiTitle());

        //Checks if use-custom-head and use-normal-material have the same value
        boolean useCustomHead = plugin.getConfig().getBoolean("discord-gui.discord-item.use-custom-head");
        boolean useNormalMaterial =  plugin.getConfig().getBoolean("discord-gui.discord-item.use-normal-material");
        if(useCustomHead && useNormalMaterial){
            error(player);
            Bukkit.getLogger().warning("[DISCORDUTILS] The value of both 'use-custom-head' and 'use-normal-material' are TRUE");
            return;
        }
        if(!useCustomHead && !useNormalMaterial){
            error(player);
            Bukkit.getLogger().warning("[DISCORDUTILS] The value of both 'use-custom-head' and 'use-normal-material' are FALSE");
            return;
        }

        //Displays the decorations if they are toggled
        boolean toggleDecorations = plugin.getConfig().getBoolean("discord-gui.decorations.toggle");
        if(toggleDecorations){
            //Getting the info of the item
            String itemString = plugin.getConfig().getString("discord-gui.decorations.material");
            String diDisplayName = plugin.getConfig().getString("discord-gui.decorations.display-name");

            //Check the material from config
            Material decoItemMat = Material.matchMaterial(itemString.toUpperCase());
            if(decoItemMat == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] The material for decoration-item is INVALID!");
                return;
            }

            ItemStack decoItem = new ItemStack(decoItemMat);
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
            //Check the material from the config
            Material iiMaterial = Material.matchMaterial(plugin.getConfig().getString("discord-gui.info-item.material").toUpperCase());
            if(iiMaterial == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] The material for info-item is INVALID!");
                return;
            }

            ItemStack infoItem = new ItemStack(iiMaterial);
            ItemMeta infoMeta = infoItem.getItemMeta();
            int infoItemSlot = plugin.getConfig().getInt("discord-gui.info-item.slot");

            //Check if the slot is valid
            if(infoItemSlot < 1 || infoItemSlot > plugin.getGuiSize()){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] The slot for info-item is INVALID!");
                return;
            }

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
            //Check the material from the config
            Material eiMaterial = Material.matchMaterial(plugin.getConfig().getString("discord-gui.exit-item.material").toUpperCase());
            if(eiMaterial == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] The material for exit-item is INVALID!");
                return;
            }

            ItemStack exitItem = new ItemStack(eiMaterial);
            ItemMeta exitMeta = exitItem.getItemMeta();

            //Check the slot for exit-item
            int exitItemSlot = plugin.getConfig().getInt("discord-gui.exit-item.slot");
            if(exitItemSlot < 1 || exitItemSlot > plugin.getGuiSize()){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS]  The slot for exit-item is INVALID!");
                return;
            }
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

        //Display the 'discord item' (with a custom-head or with a material)
        String diDisplayName = plugin.getConfig().getString("discord-gui.discord-item.display-name");

        //Check the slot for discord-item
        int diSlot =  plugin.getConfig().getInt("discord-gui.discord-item.slot");
        if(diSlot < 1 || diSlot > plugin.getGuiSize()){
            error(player);
            Bukkit.getLogger().warning("[DISCORDUTILS] The slot for discord-item is INVALID!");
            return;
        }

        if(useCustomHead){
            String customHeadValue = plugin.getConfig().getString("discord-gui.discord-item.custom-head");
            if(customHeadValue == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] Value for custom-head is NULL!");
                return;
            }

            discordItem = getHead(customHeadValue, diDisplayName);
            gui.setItem(diSlot, discordItem);
        }
        if(useNormalMaterial){
            //Check if the material is correct
            Material diMaterial = Material.matchMaterial(plugin.getConfig().getString("discord-gui.discord-item.material").toUpperCase());
            if(diMaterial == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] Material for discord-item is INVALID!");
                return;
            }

            ItemStack discordItem = new ItemStack(diMaterial);
            ItemMeta diMeta = discordItem.getItemMeta();

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

            discordItem.setItemMeta(diMeta);
            gui.setItem(diSlot, discordItem);
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

    //Manages the error task
    private void error(Player player){
        Sound errorGUI = Registry.SOUNDS.get(NamespacedKey.minecraft("open-discord-gui-sound"));
        float egVolume = plugin.getConfig().getInt("odgs-volume");
        float egPitch = plugin.getConfig().getInt("odgs-pitch");

        player.playSound(player.getLocation(), errorGUI, egVolume, egPitch);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-gui-message")));
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!(e.getView().getTitle().equals(plugin.getConfig().getString("discord-gui.title")))) return;
        e.setCancelled(true); //Doesn't let the player take or put items in the GUI

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null || clickedItem.getType().equals(Material.AIR)) return;

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;

        //If the player clicks on exit-item
        Material exitItem = Material.matchMaterial(plugin.getConfig().getString("discord-gui.exit-item.material").toUpperCase());
        if(clickedItem.getType().equals(exitItem)){
            Sound exitItemSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("exit-item-sound").toLowerCase()));
            float eisVolume = plugin.getConfig().getInt("eis-volume");
            float eisPitch = plugin.getConfig().getInt("eis-pitch");

            player.playSound(player.getLocation(), exitItemSound, eisVolume, eisPitch);
            player.closeInventory();
            return;
        }

        //If the player clicks on discord-item, runs the task.
        if(clickedItem.equals(discordItem)){
            player.closeInventory();
            plugin.getDiscordTaskManager().handleTask(player);
        }
    }
}
