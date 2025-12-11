package me.andrew.DiscordUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        configChoice = plugin.getConfig().getString("link-appearance-choice");
    }

    public void handleTask(Player player){
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
        Component discordLinkClickable;
        String discordLink = config.getString("discord-link");
        String discordWord = config.getString("discord-link-word");
        String hoverText = config.getString("hover-text");

        //Opens the book if the choice is 'book'
        if(configChoice.equalsIgnoreCase("book")){
            ItemStack discordBook = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta dbMeta =  (BookMeta) discordBook.getItemMeta();

        }

        //Sends a message in chat if the choice is 'chat-message'
        if(configChoice.equalsIgnoreCase("chat-message")){
            discordLinkClickable = Component.text(discordWord)
                    .clickEvent(ClickEvent.openUrl(discordLink))
                    .hoverEvent(HoverEvent.showText(Component.text(hoverText)));

            List<String> messageLines = config.getStringList("message-lines");
            for(String line : messageLines){
                Component compLine =  LegacyComponentSerializer.legacyAmpersand().deserialize(line);
                if(compLine.contains(Component.text("%discord_link_word%"))){
                    Component replacedLine = compLine.replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%discord_link_word%")
                            .replacement(discordLinkClickable)
                            .build());
                    replacedLine = LegacyComponentSerializer.legacyAmpersand().deserialize(String.valueOf(replacedLine));
                    player.sendMessage(replacedLine);
                }
                else{
                    player.sendMessage(compLine);
                }
            }
        }
    }

}
