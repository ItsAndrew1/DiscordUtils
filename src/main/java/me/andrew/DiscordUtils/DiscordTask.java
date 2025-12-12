//Developed by _ItsAndrew_
package me.andrew.DiscordUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class DiscordTask {
    private final DiscordUtils plugin;
    private String configChoice;

    public DiscordTask(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void handleTask(Player player){
        //Gets the appearance choice
        configChoice = plugin.getConfig().getString("link-appearance-choice");

        //Starts the 'fetching data' mini task if it is enabled
        boolean toggleFetchingData = plugin.getConfig().getBoolean("fetching-data.toggle");
        if(toggleFetchingData){
            String message = plugin.getConfig().getString("fetching-data.chat-message");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

            long duration = plugin.getConfig().getLong("fetching-data.duration");
            new BukkitRunnable() {
                public void run() {
                    giveDiscordLink(player);
                }
            }.runTaskLater(plugin, duration*20L); //Runs the task of giving the discord link later
        }
        else giveDiscordLink(player);
    }

    private void giveDiscordLink(Player player){
        FileConfiguration config = plugin.getConfig();

        //Get the giveLinkSound information
        Sound giveLinkSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("get-discord-link-sound").toLowerCase()));
        float glsVolume = plugin.getConfig().getInt("gdls-volume");
        float glsPitch = plugin.getConfig().getInt("gdls-pitch");

        //Gets the info for taskErrorSound
        Sound taskErrorSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("task-error-sound").toLowerCase()));
        float tesVolume = plugin.getConfig().getInt("tes-volume");
        float tesPitch = plugin.getConfig().getInt("tes-pitch");

        Component discordLinkClickable;
        String discordWord = config.getString("discord-link-word");
        String hoverText = config.getString("hover-text");

        //Check the discord link
        String discordLink = config.getString("discord-link");
        if(discordLink == null || discordLink.isEmpty()){
            player.playSound(player.getLocation(), taskErrorSound, tesVolume, tesPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-task-message")));
            Bukkit.getLogger().warning("[DISCORDUTILS] The discord link is null! Set one with '/dcutils setdclink <link>'!");
            return;
        }

        //Gets the appearance choice and checks if it is valid
        if(configChoice == null || configChoice.isEmpty()){ //Check if it is null
            player.playSound(player.getLocation(), taskErrorSound, tesVolume, tesPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-task-message")));
            Bukkit.getLogger().warning("[DISCORDUTILS] The value for 'link-appearance-choice' is null! Set one with '/dcutils setdcchoice <book | chat-message>");
            return;
        }
        if(!configChoice.equalsIgnoreCase("book") && !configChoice.equalsIgnoreCase("chat-message")){ //Check if it has a valid value
            player.playSound(player.getLocation(), taskErrorSound, tesVolume, tesPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-task-message")));
            Bukkit.getLogger().warning("[DISCORDUTILS] The value for 'link-appearance-choice' is invalid! Set a valid one and reload the plugin using '/dcutils reload'!");
            return;
        }
        player.playSound(player.getLocation(), giveLinkSound, glsVolume, glsPitch); //Plays the good sound if there is no error

        //Opens the book if the choice is 'book'
        if(configChoice.equalsIgnoreCase("book")){
            discordLinkClickable = Component.text(discordWord)
                    .clickEvent(ClickEvent.openUrl(discordLink))
                    .hoverEvent(HoverEvent.showText(Component.text(hoverText)));

            ItemStack discordBook = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta dbMeta =  (BookMeta) discordBook.getItemMeta();

            List<String> bookPages = config.getStringList("book-pages");
            List<Component> bookPagesComponents = new ArrayList<>();
            for(String page : bookPages){
                Component bookPage = LegacyComponentSerializer.legacyAmpersand().deserialize(page);

                //Replacing the word
                bookPage = bookPage.replaceText(TextReplacementConfig.builder()
                                .matchLiteral(discordWord)
                                .replacement(discordLinkClickable)
                        .build());

                bookPagesComponents.add(bookPage);
            }

            dbMeta.pages(bookPagesComponents);
            discordBook.setItemMeta(dbMeta);
            player.openBook(discordBook);
        }

        //Sends a message in chat if the choice is 'chat-message'
        if(configChoice.equalsIgnoreCase("chat-message")){
            discordLinkClickable = Component.text(discordWord)
                    .clickEvent(ClickEvent.openUrl(discordLink))
                    .hoverEvent(HoverEvent.showText(Component.text(hoverText)));

            List<String> messageLines = config.getStringList("message-lines");
            for(String line : messageLines){
                Component compLine =  LegacyComponentSerializer.legacyAmpersand().deserialize(line);

                //Replacing the designated word with the component
                compLine = compLine.replaceText(TextReplacementConfig.builder()
                                .matchLiteral(discordWord)
                                .replacement(discordLinkClickable)
                        .build());
                player.sendMessage(compLine);
            }
        }
    }

}
