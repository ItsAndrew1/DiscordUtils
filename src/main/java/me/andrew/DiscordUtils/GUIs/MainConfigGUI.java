package me.andrew.DiscordUtils.GUIs;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent;
import me.andrew.DiscordUtils.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.net.URI;
import java.util.UUID;

public class MainConfigGUI implements Listener {
    private final DiscordUtils plugin;
    private String GUIName;
    private ItemStack discordBlock;

    public MainConfigGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGUI(Player player){
        FileConfiguration config = plugin.getConfig();

        //Create the inventory
        GUIName = "Main Configuration";
        Inventory mainConfigGUI = Bukkit.createInventory(null, 54, GUIName);

        //Decorations
        ItemStack decoItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta diMeta = decoItem.getItemMeta();
        diMeta.setDisplayName(" ");
        decoItem.setItemMeta(diMeta);
        for(int i = 0; i<=8; i++){
            if(i==4) continue;
            mainConfigGUI.setItem(i, decoItem);
        }
        for(int i = 45; i<=53; i++){
            mainConfigGUI.setItem(i, decoItem);
        }

        //Info Item
        ItemStack infoItem = new ItemStack(Material.OAK_SIGN);
        ItemMeta iiMeta = infoItem.getItemMeta();
        iiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9&lChoose an option from below!"));
        infoItem.setItemMeta(iiMeta);
        mainConfigGUI.setItem(4, infoItem);

        //Exit Item
        ItemStack exitItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta eiMeta = exitItem.getItemMeta();
        eiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lExit"));
        exitItem.setItemMeta(eiMeta);
        mainConfigGUI.setItem(40, exitItem);

        //Manage Discord Block
        discordBlock = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta discordBlockMeta = discordBlock.getItemMeta();
        SkullMeta dbSkull = (SkullMeta) discordBlockMeta;
        //Setting the custom head for it
        PlayerProfile dbProfile = Bukkit.createProfile(UUID.randomUUID());
        dbProfile.setProperty(new ProfileProperty("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19"));
        dbSkull.setPlayerProfile(dbProfile);
        discordBlockMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lCONFIGURE THE DISCORD BLOCK"));
        discordBlock.setItemMeta(discordBlockMeta);
        mainConfigGUI.setItem(20, discordBlock);

        //Set Discord Link button
        ItemStack setDiscordLink = new ItemStack(Material.PAPER);
        ItemMeta setDiscordLinkMeta = setDiscordLink.getItemMeta();
        setDiscordLinkMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSET THE DISCORD LINK"));
        setDiscordLink.setItemMeta(setDiscordLinkMeta);
        mainConfigGUI.setItem(22, setDiscordLink);

        //Set Appearance Choice button
        ItemStack setAppearanceChoice = new ItemStack(Material.ANVIL);
        ItemMeta sacMeta =  setAppearanceChoice.getItemMeta();
        sacMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSET THE DISCORD LINK APPEARANCE"));
        setAppearanceChoice.setItemMeta(sacMeta);
        mainConfigGUI.setItem(24, setAppearanceChoice);

        player.openInventory(mainConfigGUI); //Opens the inventory
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().equals(GUIName)) return;
        event.setCancelled(true); //Disabled the player from taking/puttin items

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null || clickedItem.getType().equals(Material.OAK_SIGN) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)) return;

        String chatPrefix = plugin.getConfig().getString("chat-prefix");
        Material clickedMaterial = clickedItem.getType();
        ItemMeta ciMeta = clickedItem.getItemMeta();
        if(ciMeta == null) return;

        //Click sound
        Sound clickSound = Registry.SOUNDS.get(NamespacedKey.minecraft("ui.button.click"));

        //If the player clicks on exitItem
        Material exitItemMat = Material.RED_CONCRETE;
        if(clickedMaterial.equals(exitItemMat)){
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            player.closeInventory();
        }

        //If the player clicks on discord-link button
        Material linkSetupMat = Material.PAPER;
        if(clickedMaterial.equals(linkSetupMat)){
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            player.closeInventory();
            plugin.waitForPlayerInput(player, input->{
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the discord invite link:"));
                if(!isUrlValid(input)){
                    Sound invalidUrl = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));
                    player.playSound(player.getLocation(), invalidUrl, 1f, 1f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cThe URL is invalid! Please enter a valid one."));
                    return;
                }

                //Saves the link to config
                plugin.getConfig().set("discord-link", input);
                plugin.saveConfig();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aLink successfully set!"));
            });
        }

        //If the player clicks on block-configuration button
        if(clickedItem.equals(discordBlock)){
            player.closeInventory();
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            plugin.getBlockConfigurationGUI().showGUI(player);
        }
    }

    //Helps checking if the URL typed in by the player is valid
    private boolean isUrlValid(String url){
        try{
          URI uri = new URI(url);
          uri.toURL();
          return uri.getScheme() != null;
        } catch(Exception e){
            return false;
        }
    }
}
