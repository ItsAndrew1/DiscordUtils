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
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

    private final Map<UUID, AddingState> punishmentsAddingStates = new HashMap<>();

    private BukkitTask broadcastTask; //Task for broadcasting
    private BotMain discordBot;

    @Override
    public void onEnable(){
        saveDefaultConfig();
        startBroadcasting(); //Broadcasts the message over an interval of seconds

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
        CheckPlayerBanMute checkPlayerBanMute = new CheckPlayerBanMute(this);
        discordGUI = new DiscordGUI(this);
        discordBlockManager = new DiscordBlock(this);
        botConfig = new YMLFiles(this, "botconfig.yml");
        mainConfigGUI = new MainConfigGUI(this);
        blockConfigurationGUI = new BlockConfigurationGUI(this);
        appearanceChoiceGUI = new AppearanceChoiceGUI(this);
        facingChoiceGUI = new FacingChoiceGUI(this);

        reloadConfig();
        botConfig.reloadConfig();

        //Setting the commands and the tabs
        getCommand("discord").setExecutor(commands);
        getCommand("verify").setExecutor(commands);
        getCommand("unverify").setExecutor(commands);
        getCommand("history").setExecutor(commands);
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
        getServer().getPluginManager().registerEvents(new CommandLogSystem(this), this);
        getServer().getPluginManager().registerEvents(finalPunishmentGUI, this);
        getServer().getPluginManager().registerEvents(checkPlayerBanMute, this);
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

        //Starts the discord bot and other stuff (if the bot is toggled)
        boolean toggleDiscordBot = botConfig.getConfig().getBoolean("toggle-discord-bot", false);
        if(getConfig().getBoolean("open-discord-bot", false) && toggleDiscordBot){
            try{
                String botToken = botFile().getConfig().getString("bot-token");
                String guildId = botFile().getConfig().getString("guild-id");

                if((botToken == null || guildId == null)) Bukkit.getLogger().warning("[DISCORDUTILS] Bot Token and/or Guild ID is null. The bot won't start!");
                else discordBot = new BotMain(botToken, guildId, this);
            } catch (Exception e){
                getServer().getPluginManager().disablePlugin(this);
                Bukkit.getLogger().warning("[DISCORDUTILS] There is something wrong with the bot. The bot won't start. See message:");
                Bukkit.getLogger().warning(e.getMessage());
            }

            //Sends an embed on the verification channel, but first I check if everything is alright with the channel and the roles
            String verifiedRoleID = botFile().getConfig().getString("verification.verified-role-id");
            String unverifiedRoleID = botFile().getConfig().getString("verification.unverified-role-id");
            String verifyChannelID = botFile().getConfig().getString("verification.verify-channel-id");

            //Checking if the IDs are empty
            if(verifiedRoleID == null || unverifiedRoleID == null || verifyChannelID == null){
                Bukkit.getLogger().warning("[DISCORDUTILS] One/More IDs in 'verification' section - botconfig.yml are null!");
            }
            else{
                long verifyChannelIDlong;

                try{
                    long verifiedRoleIDlong =  Long.parseLong(verifiedRoleID);
                    long unverifiedRoleIDlong =  Long.parseLong(unverifiedRoleID);
                    verifyChannelIDlong = Long.parseLong(verifyChannelID);
                } catch (Exception e){
                    Bukkit.getLogger().warning("[DISCORDUTILS] One/More IDs in 'verification' section - botconfig.yml are invalid!");
                    Bukkit.getLogger().warning(e.getMessage());
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }

                try{
                    TextChannel verifyChannel = discordBot.getJda().getTextChannelById(verifyChannelIDlong);
                    verifyChannel.getHistory().retrievePast(1).queue(messages -> {
                        if(messages.isEmpty()){
                            //Getting the color
                            int redValue = botConfig.getConfig().getInt("verification.verify-channel-embed-color.RED", 138);
                            int blueValue = botConfig.getConfig().getInt("verification.verify-channel-embed-color.BLUE", 226);
                            int greenValue = botConfig.getConfig().getInt("verification.verify-channel-embed-color.GREEN", 43);
                            Color embedColor = Color.fromRGB(redValue, blueValue, greenValue);

                            //Getting the title and the description
                            String embedTitle = botConfig.getConfig().getString("verification.verify-channel-embed-title", "VERIFY YOUR ACCOUNT!");
                            String description = botConfig.getConfig().getString("verification.verify-channel-embed-description", "**Welcome to %server_name%!**\n\nTo start your adventure: \n1.) Head over to our **Minecraft Server**\n2.) Run */verify* to get a **code**\n3.) Come back here and run */verify <code>*\n4.) Have fun on our server!");
                            verifyChannel.sendMessageEmbeds(getEmbedBuilder(embedColor, embedTitle, description).build()).queue();
                        }
                    });
                } catch (Exception e){
                    Bukkit.getLogger().warning("[DISCORDUTILS] There was a problem when sending the embed to the 'verify' channel. See message:");
                    Bukkit.getLogger().warning(e.getMessage());
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }

            //Sending the embed in the banUsersChannel.
            String banUsersChannelIDs = botConfig.getConfig().getString("banned-users-channel.id");
            try{
                long banUsersChannelID = Long.parseLong(banUsersChannelIDs);
                TextChannel bannedUsersChannel = discordBot.getJda().getTextChannelById(banUsersChannelID);

                bannedUsersChannel.getHistory().retrievePast(1).queue(messages -> {
                    if(messages.isEmpty()){
                        //Getting the color
                        int redValue = botConfig.getConfig().getInt("banned-users-channel.embed-color.RED");
                        int greenValue = botConfig.getConfig().getInt("banned-users-channel.embed-color.GREEN");
                        int blueValue = botConfig.getConfig().getInt("banned-users-channel.embed-color.BLUE");
                        Color embedColor = Color.fromRGB(redValue, greenValue, blueValue);

                        //Getting the title and description
                        String embedTitle = botConfig.getConfig().getString("banned-users-channel.embed-title");
                        String description = botConfig.getConfig().getString("banned-users-channel.embed-description");
                        bannedUsersChannel.sendMessageEmbeds(getEmbedBuilder(embedColor, embedTitle, description).build()).addComponents(ActionRow.of(Button.primary("getbantype", "Get Your Ban ID"))).queue();
                    }
                });
            } catch(Exception e){
                Bukkit.getLogger().warning("[DISCORDUTILS] There was a problem sending the embeds in the 'banned-users-channel (botconfig.yml)'. See message:");
                Bukkit.getLogger().warning(e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            //Runs a task to auto expire the punishments
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                Connection dbConnection = databaseManager.getConnection();
                try {
                    List<Punishment> allPunishments = getAllPunishments();
                    if(!allPunishments.isEmpty()){
                        for(Punishment p : allPunishments){
                            if(p.getExpiresAt() <= System.currentTimeMillis() && p.isActive() && p.getExpiresAt() != 0){
                                String sql = "UPDATE punishments SET active = false WHERE id = ?";

                                PreparedStatement ps = dbConnection.prepareStatement(sql);
                                ps.setString(1, p.getId());
                                ps.executeUpdate();

                                PunishmentType type = p.getPunishmentType();
                                PunishmentScopes scope = p.getScope();

                                //Unbans/removed the timeout of a user if the scope is discord/global
                                UUID targetUUID = p.getUuid();
                                String sql2 = "SELECT discordId FROM playersVerification WHERE uuid = ?";
                                PreparedStatement ps2 = dbConnection.prepareStatement(sql2);
                                ps2.setString(1, targetUUID.toString());
                                ResultSet rs = ps2.executeQuery();
                                if(!rs.next()) return;

                                String userId = rs.getString("discordId");
                                if(scope == PunishmentScopes.DISCORD || scope == PunishmentScopes.GLOBAL){
                                    discordBot.getJda().retrieveUserById(userId).queue(targetUser -> {
                                        Guild dcServer = discordBot.getDiscordServer();
                                        if(type == PunishmentType.PERM_BAN || type == PunishmentType.TEMP_BAN){
                                            //Removing the banned role (and giving the 'Verified' role) from the member if he has the role
                                            long bannedRoleID = botConfig.getConfig().getLong("ban-role-id");
                                            Role bannedRole = dcServer.getRoleById(bannedRoleID);
                                            dcServer.retrieveMemberById(userId).queue(member -> {
                                                if(member.getRoles().contains(bannedRole)){
                                                    dcServer.removeRoleFromMember(member, bannedRole).queue();
                                                    dcServer.addRoleToMember(member, dcServer.getRoleById(verifiedRoleID)).queue();
                                                }
                                            });

                                        }
                                        if(type == PunishmentType.PERM_MUTE || type == PunishmentType.TEMP_MUTE){
                                            //Removes the timeout role of the member if he has the role
                                            dcServer.retrieveMemberById(userId).queue(targetMember -> {
                                                long timeoutRoleID = botConfig.getConfig().getLong("timeout-role-id");
                                                Role timeoutRole =  dcServer.getRoleById(timeoutRoleID);

                                                if(targetMember.getRoles().contains(timeoutRole)) dcServer.removeRoleFromMember(targetMember, timeoutRole).queue();
                                            });

                                            if(type == PunishmentType.TEMP_MUTE) dcServer.removeTimeout(targetUser).queue();
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
    }

    @Override
    public void onDisable() {
        saveConfig();
        if(discordBot.getJda() != null){
            Bukkit.getLogger().info("Bot shut down successfully!");
            discordBot.getJda().shutdownNow(); //Shuts down the bot if it is turned on
        }

        //Removing the cooldowns from the verification if the players have any
        Connection dbConnection = getDatabaseManager().getConnection();
        if(dbConnection != null){
            for(Player player : Bukkit.getOnlinePlayers()){
                //Checking if that player has ongoing verification
                try {
                    if(!getDatabaseManager().isPlayerVerifying(player.getUniqueId())) return;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                try {
                    UUID playerUUID = player.getUniqueId();
                    getDatabaseManager().deleteExpiredCode(playerUUID);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Bukkit.getLogger().info("DiscordUtils has been disabled successfully!");
    }

    //Quit event to remove the verification cooldown of the player.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        //Checking if that player has ongoing verification
        try {
            if(!getDatabaseManager().isPlayerVerifying(player.getUniqueId())) return;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            getDatabaseManager().deleteExpiredCode(playerUUID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public String parsePP(Player player, String text){
        if(text == null) return null;

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;

        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public void waitForPlayerInput(Player player, Consumer<String> callback){
        chatInput.put(player.getUniqueId(), callback);
    }

    //Helper method for building embeds
    private EmbedBuilder getEmbedBuilder(Color embedColor, String embedTitle, String description){
        EmbedBuilder embed = new  EmbedBuilder();
        embed.setTitle(embedTitle);
        embed.setColor(embedColor.asRGB());
        embed.setDescription(description);
        return embed;
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
        Bukkit.getLogger().info("  If you don't know how to set up the bot and all, watch the Demo Video,");
        Bukkit.getLogger().info("              as it shows a lot of important information.");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("      Also, I wrote information in both .yml files, so read thoroughly!");
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
