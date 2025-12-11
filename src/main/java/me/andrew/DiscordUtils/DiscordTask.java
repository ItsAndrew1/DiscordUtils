package me.andrew.DiscordUtils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class DiscordTask {
    private final DiscordUtils plugin;
    private String configChoice;

    public DiscordTask(DiscordUtils plugin) {
        this.plugin = plugin;
        configChoice = plugin.getConfig().getString("choice");
    }

    public void handleTask(Player player){
        FileConfiguration config = plugin.getConfig();

    }
}
