//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs;

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
        //Getting the gui size
        int guiRows = plugin.getConfig().getInt("discord-gui.rows", 6);
        if(guiRows < 0 || guiRows > 6) guiRows = 6;
        int guiSize = guiRows * 9;

        //Creating the GUI
        Inventory gui = Bukkit.createInventory(null, guiSize, plugin.getGuiTitle());

        //Displays the decorations if they are toggled
        boolean toggleDecorations = plugin.getConfig().getBoolean("discord-gui.decorations.toggle", true);
        if(toggleDecorations){
            //Getting the info of the item
            String itemString = plugin.getConfig().getString("discord-gui.decorations.material", "black_stained_glass_pane");
            String diDisplayName = plugin.getConfig().getString("discord-gui.decorations.display-name", " ");

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
                boolean toggleInfoItem = plugin.getConfig().getBoolean("discord-gui.info-item.toggle", false);
                if(toggleInfoItem){
                    int iiSlot = plugin.getConfig().getInt("discord-gui.info-item.slot", 4);
                    if(iiSlot < 0 || iiSlot > gui.getSize()) iiSlot = 4;

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
        boolean toggleInfoItem = plugin.getConfig().getBoolean("discord-gui.info-item.toggle", false);
        if(toggleInfoItem){
            //Check the material from the config
            Material iiMaterial = Material.matchMaterial(plugin.getConfig().getString("discord-gui.info-item.material", "oak_sign").toUpperCase());
            if(iiMaterial == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] The material for info-item is INVALID!");
                return;
            }

            int infoItemSlot = plugin.getConfig().getInt("discord-gui.info-item.slot", 4);
            if(infoItemSlot < 0 || infoItemSlot > gui.getSize()) infoItemSlot = 4;

            String iiDisplayName =  plugin.getConfig().getString("discord-gui.info-item.display-name");
            iiDisplayName = plugin.parsePP(player, iiDisplayName);
            List<String> infoItemLore = plugin.getConfig().getStringList("discord-gui.info-item.lore");

            ItemStack infoItem = createItem(iiMaterial, iiDisplayName, infoItemLore, player);
            gui.setItem(infoItemSlot, infoItem);
        }

        //Display the exit item if it is toggled
        boolean toggleExitItem =  plugin.getConfig().getBoolean("discord-gui.exit-item.toggle", true);
        if(toggleExitItem){
            //Check the material from the config
            Material eiMaterial = Material.matchMaterial(plugin.getConfig().getString("discord-gui.exit-item.material", "red_concrete").toUpperCase());
            if(eiMaterial == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] The material for exit-item in Discord GUI is INVALID!");
                return;
            }

            List<String> exitItemLore = plugin.getConfig().getStringList("discord-gui.exit-item.lore");
            String eiDisplayName =  plugin.getConfig().getString("discord-gui.exit-item.display-name", "&c&lEXIT");
            eiDisplayName = plugin.parsePP(player, eiDisplayName);

            ItemStack exitItem = createItem(eiMaterial, eiDisplayName, exitItemLore, player);

            //Check the slot for exit-item
            int exitItemSlot = plugin.getConfig().getInt("discord-gui.exit-item.slot", 40);
            if(exitItemSlot < 0 || exitItemSlot > gui.getSize()) exitItemSlot = 40;

            gui.setItem(exitItemSlot, exitItem);
        }

        //Display the 'discord item' (with a custom-head or with a material)
        String diDisplayName = plugin.getConfig().getString("discord-gui.discord-item.display-name", "&9Click to get the &linvite link&9!");
        diDisplayName = plugin.parsePP(player, diDisplayName);

        int diSlot =  plugin.getConfig().getInt("discord-gui.discord-item.slot", 22);
        if(diSlot < 0 || diSlot > gui.getSize()) diSlot = 22;

        boolean useCustomHead = plugin.getConfig().getBoolean("discord-gui.discord-item.use-custom-head", true);
        if(useCustomHead){
            String customHeadValue = plugin.getConfig().getString("discord-gui.discord-item.custom-head", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNiMTgzYjE0OGI5YjRlMmIxNTgzMzRhZmYzYjViYjZjMmMyZGJiYzRkNjdmNzZhN2JlODU2Njg3YTJiNjIzIn19fQ==");
            discordItem = getHead(customHeadValue, diDisplayName);
            gui.setItem(diSlot, discordItem);
        }
        else{
            //Check if the material is correct
            Material diMaterial = Material.matchMaterial(plugin.getConfig().getString("discord-gui.discord-item.material", "blue_concrete").toUpperCase());
            if(diMaterial == null){
                error(player);
                Bukkit.getLogger().warning("[DISCORDUTILS] Material for discord-item is INVALID!");
                return;
            }

            List<String> diLore =  plugin.getConfig().getStringList("discord-gui.discord-item.lore");
            ItemStack discordItem = createItem(diMaterial, diDisplayName, diLore, player);
            gui.setItem(diSlot, discordItem);
        }

        //Gets the sound from config
        Sound openGuiSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("open-discord-gui-sound").toLowerCase()));
        float ogsVolume = plugin.getConfig().getInt("odgs-volume", 1);
        float ogsPitch = plugin.getConfig().getInt("odgs-pitch", 1);

        player.playSound(player.getLocation(), openGuiSound, ogsVolume, ogsPitch); //Plays the sound from config
        player.openInventory(gui); //Opens the inventory
    }

    private ItemStack createItem(Material material, String displayName, List<String> lore, Player player){
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        //Setting the display name
        displayName = plugin.parsePP(player, displayName);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',  displayName));

        //Setting the lore
        if(lore != null) {
            //Parsing the placeholders
            List<String> parsedLore = new ArrayList<>();
            for(String loreLine : lore){
                loreLine = plugin.parsePP(player, loreLine);
                parsedLore.add(loreLine);
            }

            meta.setLore(parsedLore);
        }

        item.setItemMeta(meta);
        return item;
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
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
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
            float eisVolume = plugin.getConfig().getInt("eis-volume", 1);
            float eisPitch = plugin.getConfig().getInt("eis-pitch", 1);

            player.playSound(player.getLocation(), exitItemSound, eisVolume, eisPitch);
            player.closeInventory();
        }

        //If the player clicks on discord-item, runs the task.
        if(clickedItem.equals(discordItem)){
            player.closeInventory();
            plugin.getDiscordTaskManager().handleTask(player);
        }
    }
}
