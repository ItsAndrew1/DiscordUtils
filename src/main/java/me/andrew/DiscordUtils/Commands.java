//Developed by _ItsAndrew_
package me.andrew.DiscordUtils;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commands implements CommandExecutor {
    private final DiscordUtils plugin;

    public Commands(DiscordUtils plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) sender;
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        //Get the sounds
        Sound good = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
        Sound invalid = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));

        if(command.getName().equalsIgnoreCase("dcutils")){
            if(!sender.hasPermission("discordutils.staff")){
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-no-permission-message")));
                return true;
            }

            if(strings.length == 0){
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cUsage: &l/dcutils <setdclink | setdcchoice | reload | help>"));
                return true;
            }

            switch(strings[0]){
                //dcutils setdclink <link>
                case "setdclink":
                    if(strings.length < 2){
                        player.playSound(player.getLocation(), invalid, 1f, 1f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cUsage: &l/dcutils setdclink <link>"));
                        return true;
                    }

                    String link = strings[1];
                    plugin.getConfig().set("discord-link", link);
                    plugin.saveConfig();

                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aLink successfully saved."));
                    break;

                case "setdcchoice":
                    if(strings.length < 2){
                        player.playSound(player.getLocation(), invalid, 1f, 1f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cUsage: &l/dcutils setdcchoice <choice>"));
                        return true;
                    }

                    String choice = strings[1];
                    //Check if the choice isn't 'book' or 'chat-message'
                    if(!choice.equalsIgnoreCase("book") && !choice.equalsIgnoreCase("chat-message")){
                        player.playSound(player.getLocation(),  invalid, 1f, 1f);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cThe choice must be &lbook &cor &lchat-message&c!"));
                        return true;
                    }

                    plugin.getConfig().set("link-appearance-choice", choice);
                    plugin.saveConfig();

                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &aSaved choice &l"+choice+"&a!"));
                    break;

                case "help":
                    List<String> helpBookPages = plugin.getConfig().getStringList("help-message-book-pages");
                    ItemStack helpBook = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta hbMeta = (BookMeta) helpBook.getItemMeta();

                    //Adds the pages from config to the help book
                    for(String page : helpBookPages){
                        String coloredPage = ChatColor.translateAlternateColorCodes('&', page);
                        hbMeta.addPage(coloredPage);
                    }

                    helpBook.setItemMeta(hbMeta);
                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    player.openBook(helpBook);
                    break;

                case "reload":
                    plugin.reloadConfig();
                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &a&lDiscordUtils &asuccessfully reloaded!"));
                    break;

                default:
                    player.playSound(player.getLocation(), invalid, 1f, 1f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cInvalid command. Use &l/dcutils help &cfor info!"));
                    break;
            }
            return true;
        }

        if(command.getName().equalsIgnoreCase("discord")){
            if(!player.hasPermission("discordutils.use")){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-no-permission-message")));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                return true;
            }

            plugin.getDiscordGUI().showGUI(player);
            return true;
        }

        return false;
    }
}
