//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import me.andrew.DiscordUtils.DiscordBot.*;
import me.andrew.DiscordUtils.Plugin.GUIs.*;
import me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock.BlockConfigurationGUI;
import me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock.FacingChoiceGUI;
import me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock.MainConfigGUI;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.*;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.entities.Guild;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class DiscordUtils extends JavaPlugin implements Listener{
    private int guiSize;
    private String guiTitle;
    private DiscordGUI discordGUI;
    private DiscordTask discordTaskManager;
    private DiscordBlock discordBlockManager;
    private MainConfigGUI mainConfigGUI;
    private YMLFiles botConfig;
    private BlockConfigurationGUI blockConfigurationGUI;
    private AppearanceChoiceGUI appearanceChoiceGUI;
    private FacingChoiceGUI facingChoiceGUI;
    private VerificationManager verificationManager;
    private DatabaseManager databaseManager;
    private final Map<UUID, Consumer<String>> chatInput = new HashMap<>();

    //Punishments GUIs
    private PlayerHeadsGUIs playerHeadsGUIs;
    private AddRemoveHistoryGUI addRemovePunishmentsGUI;
    private PunishmentsGUI PunishmentsGUI;
    private ChoosePunishTypeGUI choosePunishTypeGUI;
    private ChoosePunishScopeGUI choosePunishScopeGUI;
    private FinalPunishmentGUI finalPunishmentGUI;
    private RemovePunishmentsGUI removePunishmentsGUI;

    private final Map<UUID, AddingState> punishmentsAddingStates = new HashMap<>();

    private BukkitTask broadcastTask; //Task for broadcasting
    private BotMain discordBot;

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
        Commands commands = new Commands(this);
        databaseManager = new DatabaseManager(this);
        verificationManager = new VerificationManager(this);
        playerHeadsGUIs = new PlayerHeadsGUIs(this);
        addRemovePunishmentsGUI = new AddRemoveHistoryGUI(this);
        PunishmentsGUI = new PunishmentsGUI(this);
        choosePunishTypeGUI = new ChoosePunishTypeGUI(this);
        choosePunishScopeGUI = new  ChoosePunishScopeGUI(this);
        finalPunishmentGUI = new FinalPunishmentGUI(this);
        removePunishmentsGUI = new RemovePunishmentsGUI(this);
        CheckPlayerBanMute checkPlayerBanMute = new CheckPlayerBanMute(this);
        discordGUI = new DiscordGUI(this);
        discordBlockManager = new DiscordBlock(this);
        botConfig = new YMLFiles(this, "botconfig.yml");
        mainConfigGUI = new MainConfigGUI(this);
        blockConfigurationGUI = new BlockConfigurationGUI(this);
        appearanceChoiceGUI = new AppearanceChoiceGUI(this);
        facingChoiceGUI = new FacingChoiceGUI(this);
        //Setting the commands and the tabs
        getCommand("discord").setExecutor(commands);
        getCommand("verify").setExecutor(commands);
        getCommand("unverify").setExecutor(commands);
        getCommand("dcutils").setExecutor(commands);
        getCommand("dcutils").setTabCompleter(new CommandTABS(this));

        //Setting events
        getServer().getPluginManager().registerEvents(discordGUI, this);
        getServer().getPluginManager().registerEvents(mainConfigGUI, this);
        getServer().getPluginManager().registerEvents(blockConfigurationGUI, this);
        getServer().getPluginManager().registerEvents(discordBlockManager, this);
        getServer().getPluginManager().registerEvents(appearanceChoiceGUI, this);
        getServer().getPluginManager().registerEvents(playerHeadsGUIs, this);
        getServer().getPluginManager().registerEvents(addRemovePunishmentsGUI, this);
        getServer().getPluginManager().registerEvents(PunishmentsGUI, this);
        getServer().getPluginManager().registerEvents(choosePunishTypeGUI, this);
        getServer().getPluginManager().registerEvents(choosePunishScopeGUI, this);
        getServer().getPluginManager().registerEvents(finalPunishmentGUI, this);
        getServer().getPluginManager().registerEvents(checkPlayerBanMute, this);
        getServer().getPluginManager().registerEvents(removePunishmentsGUI, this);
        getServer().getPluginManager().registerEvents(facingChoiceGUI, this);
        getServer().getPluginManager().registerEvents(this, this);

        //Runs a 'first-run' message if the plugin is run for the first time
        if(!getConfig().getBoolean("initialized", false)){
            firstRunSetup();
            getConfig().set("initialized", true);
            saveConfig();
        }
        else{ //Spawns the discord-block and starts the particle task if the plugin is not run for the first time.
            getDiscordBlockManager().spawnDiscordBlock();
            getDiscordBlockManager().startParticleTask();
        }

        //Creates the database
        try {
            databaseManager.connectDb();
            Bukkit.getLogger().info("[DISCORDUTILS] Successfully created database connection.");
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[DISCORDUTILS] Failed to create database connection. See message: ");
            Bukkit.getLogger().warning("[DISCORDUTILS]: "+e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }

        //Starts the discord bot if everything is ok
        try{
            String botToken = botFile().getConfig().getString("bot-token");
            String guildId = botFile().getConfig().getString("guild-id");

            if(botToken == null || guildId == null){
                Bukkit.getLogger().warning("[DISCORDUTILS] Bot Token and/or Guild ID is null. The bot won't start!");
            }
            else discordBot = new BotMain(botToken, guildId, this);
        } catch (Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] There is something wrong with the bot. The bot won't start. See message:");
            Bukkit.getLogger().warning(e.getMessage());
        }

        //Runs a task to auto expire the punishments
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            Connection dbConnection = databaseManager.getConnection();
            try {
                List<Punishment> allPunishments = getAllPunishments();
                if(!allPunishments.isEmpty()){
                    for(Punishment p : allPunishments){
                        if(p.getExpiresAt() <= System.currentTimeMillis() && p.isActive()){
                            String sql = "UPDATE punishments SET active = false WHERE id = ?";

                            PreparedStatement ps = dbConnection.prepareStatement(sql);
                            ps.setString(1, p.getId());
                            ps.executeUpdate();

                            PunishmentType type = p.getPunishmentType();
                            PunishmentScopes scope = p.getScope();

                            String expireTimeoutDM = botConfig.getConfig().getString("user-punishments-messages.timeout-expire-user-message");
                            String expireBanDM = botConfig.getConfig().getString("user-punishments-messages.ban-expire-user-message");

                            //Getting the user ID from the target UUID assigned to the punishment
                            UUID targetUUID = p.getUuid();
                            //Sends the target user a DM about this expiration if the user is verified
                            if(databaseManager.isVerified(targetUUID)){
                                String sql2 = "SELECT discordId FROM playersVerification WHERE uuid = ?";
                                PreparedStatement ps2 = dbConnection.prepareStatement(sql2);
                                ps2.setString(1, targetUUID.toString());
                                ResultSet rs = ps2.executeQuery();
                                String userId = rs.getString("discordId");

                                discordBot.getJda().retrieveUserById(userId).queue(targetUser -> {
                                    Guild dcServer = discordBot.getDiscordServer();
                                    if(type == PunishmentType.PERM_BAN || type == PunishmentType.TEMP_BAN){
                                        targetUser.openPrivateChannel().queue(channel -> {
                                            String banType = type.isPermanent() ? "PERMANENT BAN" : "TEMPORARY BAN";
                                            channel.sendMessage(expireBanDM
                                                    .replace("%scope%", scope.name())
                                                    .replace("%ban_type%", banType)
                                                    .replace("%user%", targetUser.getName())
                                                    .replace("%server_name%", dcServer.getName())
                                            ).queue(success -> dcServer.unban(targetUser).queue(), failure -> dcServer.unban(targetUser).queue());
                                        }, failure -> dcServer.unban(targetUser).queue());
                                    }

                                    if(type == PunishmentType.PERM_MUTE || type == PunishmentType.TEMP_MUTE){
                                        targetUser.openPrivateChannel().queue(channel -> {
                                            String muteType =  type.isPermanent() ? "PERMANENT MUTE" : "TEMPORARY MUTE";
                                            channel.sendMessage(expireTimeoutDM
                                                    .replace("%user%", targetUser.getName())
                                                    .replace("%server_name%", dcServer.getName())
                                                    .replace("%scope%", scope.name())
                                                    .replace("%timeout_type%", muteType)
                                            ).queue(success -> dcServer.removeTimeout(targetUser).queue(), failure -> dcServer.removeTimeout(targetUser).queue());
                                        }, failure -> dcServer.removeTimeout(targetUser).queue());
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, 0L, 20L*5); //Runs every 5 seconds
    }

    @Override
    public void onDisable() {
        saveConfig();
        if(discordBot.getJda() != null){
            Bukkit.getLogger().info("Bot shut down successfully!");
            discordBot.getJda().shutdownNow(); //Shuts down the bot if it is turned on
        }

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

    //Method for getting a list of all Punishments (List<Punishment>)
    private List<Punishment> getAllPunishments() throws SQLException{
        Connection dbConnection = getDatabaseManager().getConnection();
        List<Punishment> allPunishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    Punishment p = getDatabaseManager().mapPunishment(rs);
                    allPunishments.add(p);
                }
                return allPunishments;
            }
        }
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

    //Helps to format the time
    public String formatTime(long millis){
        long days =  TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        long hours =  TimeUnit.MILLISECONDS.toHours(millis);
        millis -=  TimeUnit.HOURS.toMillis(hours);

        long minutes =  TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds =  TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if(minutes > 0) seconds = 0;

        if(days > 0) sb.append(days).append("d ");
        if(hours > 0) sb.append(hours).append("h ");
        if(minutes > 0) sb.append(minutes).append("m ");
        if(seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    //Message that runs on the first setup
    private void firstRunSetup(){
        Bukkit.getLogger().info("=========================================================================");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("             DiscordUtils has been initialized successfully!");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("IMPORTANT: To toggle on the discord-block, run /dcutils configuration,");
        Bukkit.getLogger().info("          configure the block, and then run /dcutils reload!");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("ALSO IMPORTANT: For the discord bot, go into config.yml and configure the bot.");
        Bukkit.getLogger().info("         Then, restart the server. More instructions are there!");
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
    public YMLFiles botFile() {
        return botConfig;
    }
    public PlayerHeadsGUIs getPlayerHeadsGUIs() {
        return playerHeadsGUIs;
    }
    public AddRemoveHistoryGUI getAddRemovePunishGUI() {
        return addRemovePunishmentsGUI;
    }
    public PunishmentsGUI getPunishmentsGUI() {
        return PunishmentsGUI;
    }
    public ChoosePunishTypeGUI getChoosePunishTypeGUI() {
        return choosePunishTypeGUI;
    }
    public FinalPunishmentGUI getFinalPunishmentGUI() {
        return finalPunishmentGUI;
    }
    public ChoosePunishScopeGUI getChoosePunishScopeGUI() {
        return choosePunishScopeGUI;
    }
    public RemovePunishmentsGUI getRemovePunishmentsGUI() {
        return removePunishmentsGUI;
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
    public Map<UUID, AddingState> getPunishmentsAddingStates(){
        return punishmentsAddingStates;
    }

    public BotMain getDiscordBot() {
        return discordBot;
    }
}
