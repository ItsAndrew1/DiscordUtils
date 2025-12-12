//Developed by _ItsAndrew_
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
            Bukkit.getLogger().warning("[DISCORDUTILS] The value of 'discord-gui.rows' is invalid! The GUI won't show up.");
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
        Bukkit.getLogger().info("DiscordUtils has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        saveConfig();

        Bukkit.getLogger().info("DiscordUtils has been disabled successfully!");
    }

    //Handles the broadcasting task
    private void startBroadcasting(){
        //Check if broadcasting is toggled
        boolean toggleBroadcasting = getConfig().getBoolean("autobroadcast.toggle");
        if(!toggleBroadcasting) return;

        //Check if the interval is valid
        long interval;
        String intervalString = getConfig().getString("autobroadcast.interval");
        try{
            interval =  Long.parseLong(intervalString);
            if(interval <= 0){
                Bukkit.getLogger().warning("[DISCORDUTILS] Could not start broadcasting task. The interval value is <= 0!");
                return;
            }
        }catch(Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] Could not start broadcasting task. The interval value is invalid!");
            return;
        }

        Sound broadcastSound = Registry.SOUNDS.get(NamespacedKey.minecraft(getConfig().getString("boardcast-message-sound").toLowerCase()));

        //Check if sound is null
        if(broadcastSound == null) Bukkit.getLogger().warning("[DISCORDUTILS] Sound for broadcast is invalid!");
        float bcsVolume = getConfig().getInt("bms-volume");
        float bcsPitch = getConfig().getInt("bms-pitch");

        //Starts the task
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
