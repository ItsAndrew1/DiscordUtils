//Developed by _ItsAndrew_
package me.andrew.DiscordUtils;

import me.andrew.DiscordUtils.GUIs.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class DiscordUtils extends JavaPlugin implements Listener{
    private int guiSize;
    private String guiTitle;
    private DiscordGUI discordGUI;
    private DiscordTask discordTaskManager;
    private Commands commands;
    private DiscordBlock discordBlockManager;
    private MainConfigGUI mainConfigGUI;
    private BlockConfigurationGUI blockConfigurationGUI;
    private AppearanceChoiceGUI appearanceChoiceGUI;
    private FacingChoiceGUI facingChoiceGUI;
    private PlayerJoin playerJoin;
    private VerificationManager verificationManager;
    private DatabaseManager databaseManager;
    private final Map<UUID, Consumer<String>> chatInput = new HashMap<>();

    private BukkitTask broadcastTask; //Task for broadcasting

    @Override
    public void onEnable(){
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
        commands = new Commands(this);
        databaseManager = new DatabaseManager(this);
        verificationManager = new VerificationManager(this);
        discordGUI = new DiscordGUI(this);
        playerJoin = new PlayerJoin(this);
        discordBlockManager = new DiscordBlock(this);
        mainConfigGUI = new MainConfigGUI(this);
        blockConfigurationGUI = new BlockConfigurationGUI(this);
        appearanceChoiceGUI = new AppearanceChoiceGUI(this);
        facingChoiceGUI = new FacingChoiceGUI(this);

        //Setting the commands and the tabs
        getCommand("discord").setExecutor(commands);
        getCommand("verify").setExecutor(commands);
        getCommand("dcutils").setExecutor(commands);
        getCommand("dcutils").setTabCompleter(new CommandTABS(this));

        //Setting events
        getServer().getPluginManager().registerEvents(discordGUI, this);
        getServer().getPluginManager().registerEvents(mainConfigGUI, this);
        getServer().getPluginManager().registerEvents(blockConfigurationGUI, this);
        getServer().getPluginManager().registerEvents(discordBlockManager, this);
        getServer().getPluginManager().registerEvents(appearanceChoiceGUI, this);
        getServer().getPluginManager().registerEvents(facingChoiceGUI, this);
        getServer().getPluginManager().registerEvents(playerJoin, this);
        getServer().getPluginManager().registerEvents(this, this);

        //Runs a 'first-run' message if the plugin is run for the first time
        if(!getConfig().getBoolean("initialized", false)){
            firstRunSetup();
            try {
                databaseManager.createDb();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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

    //Handles the chat input for different configurations of the plugin
    @EventHandler
    public void chatAsync(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if(!chatInput.containsKey(playerUUID)) return;
        event.setCancelled(true);

        String message = event.getMessage();
        Consumer<String> callback = chatInput.remove(playerUUID);
        Bukkit.getScheduler().runTask(this, () -> callback.accept(message));
    }

    public void waitForPlayerInput(Player player, Consumer<String> callback){
        chatInput.put(player.getUniqueId(), callback);
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
        //Displays a message
        Bukkit.getLogger().info("=========================================================================");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("             DiscordUtils has been initialized successfully!");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("IMPORTANT: To toggle on the discord-block, run /dcutils configuration,");
        Bukkit.getLogger().info("          configure the block, and then run /dcutils reload!");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("                 Thank you for using DiscordUtils!");
        Bukkit.getLogger().info(" ");
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
    public BlockConfigurationGUI getBlockConfigurationGUI() {
        return  blockConfigurationGUI;
    }
    public MainConfigGUI getMainConfigGUI() {
        return mainConfigGUI;
    }
    public FacingChoiceGUI getFacingChoiceGUI() {
        return facingChoiceGUI;
    }
    public AppearanceChoiceGUI getAppearanceChoiceGUI() {
        return appearanceChoiceGUI;
    }
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    public VerificationManager getVerificationManager() {
        return verificationManager;
    }
    public int getGuiSize() {
        return guiSize;
    }
    public String getGuiTitle() {
        return guiTitle;
    }
}
