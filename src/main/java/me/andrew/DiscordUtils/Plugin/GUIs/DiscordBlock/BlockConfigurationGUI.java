//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

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

        //Info Item
        ItemStack infoItem = new ItemStack(Material.OAK_SIGN);
        ItemMeta iiMeta = infoItem.getItemMeta();
        iiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9&lCONFIGURE THE DISCORD-BLOCK"));
        List<String> coloredIiLore = new ArrayList<>();
        coloredIiLore.add(ChatColor.translateAlternateColorCodes('&', "&7• Set the &llocation &7of the block"));
        coloredIiLore.add(ChatColor.translateAlternateColorCodes('&', "&7• Set the &lworld &7of the block"));
        coloredIiLore.add(ChatColor.translateAlternateColorCodes('&', "&7• Set the &lfacing &7of the block"));
        iiMeta.setLore(coloredIiLore);
        infoItem.setItemMeta(iiMeta);
        gui.setItem(4, infoItem);

        //Set Location Button
        ItemStack locationButton = new ItemStack(Material.COMPASS);
        ItemMeta lbMeta = locationButton.getItemMeta();
        lbMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSET THE LOCATION"));
        locationButton.setItemMeta(lbMeta);
        gui.setItem(20, locationButton);

        //Set World Button
        ItemStack worldButton = new ItemStack(Material.MAP);
        ItemMeta wbMeta = worldButton.getItemMeta();
        wbMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSET THE WORLD"));
        List<String> coloredWbLore = new ArrayList<>();
        coloredWbLore.add(" ");
        coloredWbLore.add(ChatColor.translateAlternateColorCodes('&', "&e&lTIP: &eLook through your server folders to see the name of the world!"));
        coloredWbLore.add(ChatColor.translateAlternateColorCodes('&', "&eUsually they are called &lworld&e/&lworld_nether&e/&lworld_the_end&e!"));
        wbMeta.setLore(coloredWbLore);
        worldButton.setItemMeta(wbMeta);
        gui.setItem(22,  worldButton);

        //Set Facing Button
        ItemStack facingButton = new ItemStack(Material.ITEM_FRAME);
        ItemMeta fbMeta = facingButton.getItemMeta();
        fbMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSET THE FACING"));
        List<String> coloredFbLore = new ArrayList<>();
        coloredFbLore.add(" ");
        coloredFbLore.add(ChatColor.translateAlternateColorCodes('&', "&eThis is for setting the block's rotation."));
        fbMeta.setLore(coloredFbLore);
        facingButton.setItemMeta(fbMeta);
        gui.setItem(24, facingButton);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().equals(guiTitle)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null || clickedItem.getType().equals(Material.OAK_SIGN) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)){
            event.setCancelled(true);
            return;
        }

        Material clickedMaterial = clickedItem.getType();
        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) {
            event.setCancelled(true);
            return;
        }
        Sound clickSound = Registry.SOUNDS.get(NamespacedKey.minecraft("ui.button.click"));
        Sound inputSound = Registry.SOUNDS.get(NamespacedKey.minecraft("block.note_block.pling"));
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        //If the player clicks on return item
        Material returnItem =  Material.RED_CONCRETE;
        if(clickedMaterial == returnItem){
            event.setCancelled(true);
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            plugin.getMainConfigGUI().showGUI(player);
            return;
        }

        //If the player clicks on world button
        Material worldButton = Material.MAP;
        if(clickedMaterial == worldButton){
            event.setCancelled(true);
            player.closeInventory();
            player.playSound(player.getLocation(), inputSound, 1f, 1f);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the &lworld &afor the block: "));
            plugin.waitForPlayerInput(player, input->{
                plugin.getConfig().set("block-world", input);
                plugin.saveConfig();

                Sound goodWorld = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
                player.playSound(player.getLocation(), goodWorld, 1f, 1.4f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aSaved world &l"+input+" &afor the discord-block!"));

                //Re-opens the GUI after 1/2 seconds
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        showGUI(player);
                    }
                }.runTaskLater(plugin, 10L);
            });
        }

        //If the player clicks on facing button
        Material facingButton = Material.ITEM_FRAME;
        if(clickedMaterial == facingButton){
            event.setCancelled(true);
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            plugin.getFacingChoiceGUI().showGui(player);
            return;
        }

        //If the player clicks on location button
        Material locationButton = Material.COMPASS;
        if(clickedMaterial == locationButton){
            event.setCancelled(true);
            player.closeInventory();
            player.playSound(player.getLocation(), inputSound, 1f, 1f);

            //Coordinate X
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the coordinate X:"));
            plugin.waitForPlayerInput(player, input1->{
                //Checks if the coordinate X is valid
                double coordinateX;
                try{
                    coordinateX = Double.parseDouble(input1);
                    plugin.getConfig().set("block-x", coordinateX);
                    plugin.saveConfig();
                } catch (Exception e){
                    Sound invalidCoordinate = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));
                    player.playSound(player.getLocation(), invalidCoordinate, 1f, 1f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cInvalid coordinate X."));

                    //Opens the block config GUI again after 1/2 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.getBlockConfigurationGUI().showGUI(player);
                        }
                    }.runTaskLater(plugin, 10L);
                    return;
                }

                player.playSound(player.getLocation(), inputSound, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the coordinate Y:"));

                //Coordinate Y
                plugin.waitForPlayerInput(player, input2->{
                    //Check if the coordinate is valid
                    double coordinateY;
                    try{
                        coordinateY = Double.parseDouble(input2);
                        plugin.getConfig().set("block-y", coordinateY);
                        plugin.saveConfig();
                    } catch (Exception e){
                        Sound InvalidCoordinate = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));
                        player.playSound(player.getLocation(), InvalidCoordinate, 1f, 1f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cInvalid coordinate Y."));

                        //Opens the block config GUI again after 1/2 seconds
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                plugin.getBlockConfigurationGUI().showGUI(player);
                            }
                        }.runTaskLater(plugin, 10L);
                        return;
                    }

                    player.playSound(player.getLocation(), inputSound, 1f, 1f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the coordinate Z:"));

                    //Coordinate Z
                    plugin.waitForPlayerInput(player, input3->{
                        double coordinateZ;
                        //Check if the coordinate is valid
                        try{
                            coordinateZ = Double.parseDouble(input3);
                            plugin.getConfig().set("block-z", coordinateZ);
                            plugin.saveConfig();
                        } catch (Exception e){
                            Sound invalidCoord = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));
                            player.playSound(player.getLocation(), invalidCoord, 1f, 1f);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cInvalid coordinate Z."));

                            //Opens the block config GUI again after 1/2 seconds
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    plugin.getBlockConfigurationGUI().showGUI(player);
                                }
                            }.runTaskLater(plugin, 10L);
                            return;
                        }

                        Sound validCoords = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
                        player.playSound(player.getLocation(), validCoords, 1f, 1.4f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', " &aLocation &l"+input1+" "+input2+" "+input3+" &asaved for discord block."));

                        //Opens the block config GUI again after 1/2 seconds
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                plugin.getBlockConfigurationGUI().showGUI(player);
                            }
                        }.runTaskLater(plugin, 10L);
                    });
                });
            });
        }
    }
}
