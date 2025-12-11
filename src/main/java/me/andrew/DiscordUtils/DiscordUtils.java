package me.andrew.DiscordUtils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class DiscordUtils extends JavaPlugin {
    private int guiSize;
    private String guiTitle;
    private DiscordGUI discordGUI;
    private DiscordTask discordTaskManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startBroadcasting(); //Broadcasts the message over an interval of seconds

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

    private void startBroadcasting(){
        //Check if broadcasting is toggled
        boolean toggleBroadcasting = getConfig().getBoolean("autobroadcast.toggle");
        if(!toggleBroadcasting) return;

        long interval = getConfig().getLong("autobroadcast.interval");
        Sound broadcastSound = Registry.SOUNDS.get(NamespacedKey.minecraft(getConfig().getString("boardcast-message-sound").toLowerCase()));
        float bcsVolume = getConfig().getInt("bms-volume");
        float bcsPitch = getConfig().getInt("bms-pitch");
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            List<String> broadcastLines = getConfig().getStringList("autobroadcast.message-lines");
            for(Player player : Bukkit.getOnlinePlayers()){
                for(String line : broadcastLines){
                    String coloredLine =  ChatColor.translateAlternateColorCodes('&', line);
                    player.sendMessage(coloredLine);
                }
                player.playSound(player.getLocation(), broadcastSound, bcsVolume, bcsPitch);
            }
        }, 20L * interval, 20L * interval);
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
