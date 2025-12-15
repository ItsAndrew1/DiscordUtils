//Developed by _ItsAndrew_
package me.andrew.DiscordUtils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class DiscordUtils extends JavaPlugin {
    private int guiSize;
    private String guiTitle;
    private DiscordGUI discordGUI;
    private DiscordTask discordTaskManager;
    private DiscordBlock discordBlockManager;

    private boolean blockConfigured;
    private boolean particleTaskFirstTime;
    private BukkitTask broadcastTask;

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
        discordBlockManager = new DiscordBlock(this);

        //Setting the commands and the tabs
        getCommand("discord").setExecutor(new Commands(this));
        getCommand("dcutils").setExecutor(new Commands(this));
        getCommand("dcutils").setTabCompleter(new CommandTABS(this));

        //Setting events
        getServer().getPluginManager().registerEvents(discordGUI, this);

        //Runs a 'first-run' message if the plugin is run for the first time
        if(!getConfig().getBoolean("initialized", false)){
            firstRunSetup();
            setParticleTaskFirstTime(true);
            getConfig().set("initialized", true);
            saveConfig();
        }
        else{ //Spawns the discord-block and starts the particle task if the plugin is not run for the first time.
            getDiscordBlockManager().spawnDiscordBlock();
            getDiscordBlockManager().startParticleTask();
        }
    }

    @Override
    public void onDisable() {
        saveConfig();

        Bukkit.getLogger().info("DiscordUtils has been disabled successfully!");
    }

    //Handles the broadcasting task
    public void startBroadcasting(){
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

        broadcastTask = Bukkit.getScheduler().runTaskTimer(this, ()->{
            List<String> broadcastLines = getConfig().getStringList("autobroadcast.message-lines");
            for(Player player : Bukkit.getOnlinePlayers()){
                for(String line : broadcastLines){
                    String coloredLine =  ChatColor.translateAlternateColorCodes('&', line);
                    player.sendMessage(coloredLine);
                }
                player.playSound(player.getLocation(), broadcastSound, bcsVolume, bcsPitch);
            }
        }, 20L*interval, 20L*interval);
    }

    //Message that runs on the first setup
    private void firstRunSetup(){
        Bukkit.getLogger().info("=========================================================================");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("             DiscordUtils has been initialized successfully!");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("IMPORTANT: To toggle on the discord-block, run /dcutils blockconfig,");
        Bukkit.getLogger().info("          configure the block, and then run /dcutils reload!");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("                 Thank you for using DiscordUtils!");
        Bukkit.getLogger().info("=========================================================================");
    }

    //Getters
    public BukkitTask getBroadcastTask() {
        return broadcastTask;
    }
    public DiscordTask getDiscordTaskManager() {
        return  discordTaskManager;
    }
    public DiscordBlock getDiscordBlockManager() {
        return  discordBlockManager;
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

    //Getter/Setter for particleTaskFirstTime
    public void setParticleTaskFirstTime(boolean value) {
        particleTaskFirstTime = value;
    }
    public boolean isParticleTaskFirstTime() {
        return particleTaskFirstTime;
    }
}
