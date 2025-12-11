package me.andrew.DiscordUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordUtils extends JavaPlugin {
    private int guiSize;
    private String guiTitle;
    private DiscordGUI discordGUI;
    private DiscordTask discordTaskManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        //Checks the GUI size
        int guiRows = getConfig().getInt("discord-gui.rows");
        if(guiRows < 1 || guiRows > 6) {
            Bukkit.getLogger().warning("[DISCORDUTILS] The value of 'discord-gui.rows' is invalid!");
        }
        guiSize = guiRows * 9; //Sets the GUI size

        guiTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("discord-gui.title"));
        discordTaskManager = new DiscordTask(this);
        discordGUI = new DiscordGUI(this);

        //Setting the commands and the tabs
        getCommand("discord").setExecutor(new Commands(this));
        getCommand("dcutils").setExecutor(new Commands(this));
        getCommand("dcutils").setTabCompleter(new CommandTABS(this));

        //Setting events
        getServer().getPluginManager().registerEvents(discordGUI, this);
    }

    @Override
    public void onDisable() {
        saveConfig();

        Bukkit.getLogger().info("DiscordUtils has been disabled successfully!");
    }

    //Getters
    public DiscordTask getDiscordTaskManager() {
        return  discordTaskManager;
    }
    public DiscordGUI getDiscordGUI() {
        return discordGUI;
    }
    public int getGuiSize() {
        return guiSize;
    }
    public String getGuiTitle() {
        return guiTitle;
    }
}
