//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DiscordTask {
    private final DiscordUtils plugin;

    public DiscordTask(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void handleTask(Player player){ //This is mainly used for the discord GUI
        //Starts the 'fetching data' mini task if it is enabled
        boolean toggleFetchingData = plugin.getConfig().getBoolean("fetching-data.toggle");
        if(!toggleFetchingData) giveDiscordLink(player);
        else{
            //Check if the player has cooldown
            if(plugin.getDiscordBlockManager().getCooldowns().contains(player.getUniqueId())){
                //Plays the sound
                Sound taskInProgress = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("fetching-data-task-in-progress-sound").toLowerCase()));
                float fdtipsVolume = plugin.getConfig().getInt("fdtips-volume");
                float fdtipsPitch = plugin.getConfig().getInt("fdtips-pitch");
                player.playSound(player.getLocation(), taskInProgress, fdtipsVolume, fdtipsPitch);

                //Sends the message from the config
                String taskInProgressMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("fetching-data-task-in-progress-message"));
                player.sendMessage(taskInProgressMessage);
                return;
            }

            //Adds the player to the Cooldowns Set
            plugin.getDiscordBlockManager().getCooldowns().add(player.getUniqueId());

            //Sends the fetching data chat message
            String fdChatMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("fetching-data-chat-message"));
            player.sendMessage(fdChatMessage);

            int duration = plugin.getConfig().getInt("fetching-data.duration");
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getDiscordBlockManager().getCooldowns().remove(player.getUniqueId()); //Removes the player from the cooldown Set
                    giveDiscordLink(player);
                }
            }.runTaskLater(plugin, duration*20L);
        }
    }

    public void giveDiscordLink(Player player){
        FileConfiguration config = plugin.getConfig();

        //Gets the appearance choice
        String configChoice = plugin.getConfig().getString("link-appearance-choice");

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
        if(discordLink == null || discordLink.isEmpty() || !isUrlValid(discordLink)){
            player.playSound(player.getLocation(), taskErrorSound, tesVolume, tesPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-task-message")));
            Bukkit.getLogger().warning("[DISCORDUTILS] The discord link is null or invalid! Run /dcutils configuration and set a valid one!");
            return;
        }

        //Gets the appearance choice and checks if it is valid
        if(configChoice == null || configChoice.isEmpty()){ //Check if it is null
            player.playSound(player.getLocation(), taskErrorSound, tesVolume, tesPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-task-message")));
            Bukkit.getLogger().warning("[DISCORDUTILS] The value for 'link-appearance-choice' is null! Run /dcutils configuration and set it!");
            return;
        }
        if(!configChoice.equalsIgnoreCase("book") && !configChoice.equalsIgnoreCase("chat-message")){ //Check if it has a valid value
            player.playSound(player.getLocation(), taskErrorSound, tesVolume, tesPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("error-task-message")));
            Bukkit.getLogger().warning("[DISCORDUTILS] The value for 'link-appearance-choice' is invalid! Run /dcutils configuration and set a valid one!");
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
