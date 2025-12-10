package me.andrew.DiscordUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordUtils extends JavaPlugin {
    private Inventory discordGUI;
    private int guiSize;
    private String guiTitle;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        guiSize = getConfig().getInt("discord-gui.rows") * 9; //Sets the GUI size
        guiTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("discord-gui.title"));

        //Setting the commands and the tabs
        getCommand("discord").setExecutor(new Commands(this));
        getCommand("discord").setTabCompleter(new CommandTABS(this));
        getCommand("discordlink").setExecutor(new Commands(this));

        //Checks certain conditions

    }

    @Override
    public void onDisable() {
        saveConfig();

        Bukkit.getLogger().warning("DiscordLink has been disabled successfully!");
    }

    //Getters
    public Inventory getDiscordGUI() {
        return discordGUI;
    }
    public int getGuiSize() {
        return guiSize;
    }
    public String getGuiTitle() {
        return guiTitle;
    }
}
