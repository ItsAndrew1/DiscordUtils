//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import me.andrew.DiscordUtils.Caching.PlayerVerificationCache;
import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.DiscordBot.*;
import me.andrew.DiscordUtils.Plugin.GUIs.*;
import me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock.BlockConfigurationGUI;
import me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock.FacingChoiceGUI;
import me.andrew.DiscordUtils.Plugin.GUIs.DiscordBlock.MainConfigGUI;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.*;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Caching.PlayerPunishmentDataCache;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class DiscordUtils extends JavaPlugin implements Listener{
    private DiscordGUI discordGUI;
    private DiscordTask discordTaskManager;
    private DiscordBlock discordBlockManager;
    private MainConfigGUI mainConfigGUI;
    private YMLFiles botConfig;
    private BlockConfigurationGUI blockConfigurationGUI;
    private AppearanceChoiceGUI appearanceChoiceGUI;
    private FacingChoiceGUI facingChoiceGUI;
    private VerificationManager verificationManager;
    private volatile DatabaseManager databaseManager;
    private final Map<UUID, Consumer<String>> chatInput = new ConcurrentHashMap<>();

    //Punishments GUIs
    private PlayerHeadsGUIs playerHeadsGUIs;
    private AddRemoveHistoryGUI addRemovePunishmentsGUI;
    private PunishmentsGUI PunishmentsGUI;
    private ChoosePunishTypeGUI choosePunishTypeGUI;
    private ChoosePunishScopeGUI choosePunishScopeGUI;
    private FinalPunishmentGUI finalPunishmentGUI;

    private final Map<UUID, AddingState> punishmentsAddingStates = new HashMap<>();
    private final PlayerPunishmentDataCache playerPunishmentsCache = new PlayerPunishmentDataCache(this);
    private final PlayerVerificationCache playerVerificationCache = new PlayerVerificationCache();

    private BukkitTask broadcastTask; //Task for broadcasting
    private volatile BotMain discordBot;

    @Override
    public void onEnable(){
        saveDefaultConfig();

        discordTaskManager = new DiscordTask(this);
        Commands commands = new Commands(this);
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

        //Setting the commands and the tabs
        getCommand("discord").setExecutor(commands);
        getCommand("history").setExecutor(commands);
        getCommand("dcutils").setExecutor(commands);
        getCommand("verify").setExecutor(commands);
        getCommand("unverify").setExecutor(commands);
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

        startBroadcasting(); //Broadcasts the message over an interval of seconds

        // JDBC, cache warm-up and JDA login must not run on the server thread.
        Bukkit.getScheduler().runTaskAsynchronously(this, this::startNetworkServices);
    }

    private void startNetworkServices(){
        try{
            databaseManager = new DatabaseManager(this);
        } catch (Exception e){
            getLogger().severe("[DISCORDUTILS] Database setup failed: " + e.getMessage());
            disableOnMainThread();
            return;
        }

        try{
            if(!startDiscordBot()) return;
            warmCaches();
            startPunishmentExpiryTask();
        } catch (Exception e){
            getLogger().warning("[DISCORDUTILS] There is something wrong with the bot or the cache. See message:");
            getLogger().warning(e.getMessage());
            disableOnMainThread();
        }
    }

    /**
     * Logs in JDA. Returns false when the bot is turned off in config.
     * Callers must already be off the main thread, because {@code awaitReady()} blocks.
     */
    private boolean startDiscordBot() throws Exception{
        boolean openDiscordBot = getConfig().getBoolean("open-discord-bot", false);
        boolean toggleDiscordBot = botConfig.getConfig().getBoolean("toggle-discord-bot", false);
        if(!openDiscordBot || !toggleDiscordBot) return false;

        String botToken = botFile().getConfig().getString("bot-token");
        String guildId = botFile().getConfig().getString("guild-id");

        if(botToken == null || botToken.isBlank() || guildId == null || guildId.isBlank()){
            getLogger().warning("[DISCORDUTILS] Bot Token and/or Guild ID is null. The bot won't start!");
            return false;
        }

        discordBot = new BotMain(botToken, guildId, this);
        if(discordBot.getJda() == null || discordBot.getDiscordServer() == null) return false;

        sendEmbedInVerificationChannel();
        sendEmbedInBannedChannel();
        return true;
    }

    public void restartDiscordBot(){
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            BotMain current = discordBot;
            discordBot = null;
            if(current != null && current.getJda() != null) current.getJda().shutdownNow();

            botConfig.reloadConfig();
            try{
                if(!startDiscordBot()) getLogger().warning("[DISCORDUTILS] The bot did not start after reload.");
            } catch (Exception e){
                getLogger().warning("[DISCORDUTILS] Couldn't reload the bot! See message: " + e.getMessage());
            }
        });
    }

    private void warmCaches() throws SQLException{
        for(OfflinePlayer player : Bukkit.getOfflinePlayers()){
            CopyOnWriteArrayList<Punishment> playerPunishments = getAllPunishments(player.getUniqueId());
            getPlayerPunishmentDataCache().addPlayerPunishments(player.getUniqueId(), playerPunishments);
        }

        ConcurrentHashMap<UUID, String> verifiedData = getDatabaseManager().getVerifiedData();
        for(Map.Entry<UUID, String> entry : verifiedData.entrySet()){
            UUID uuid = entry.getKey();
            String discordId = entry.getValue();

            getPlayerVerificationCache().putDiscordIdUUID(uuid, discordId);
            getPlayerVerificationCache().putUuidDiscordID(uuid, discordId);
        }
    }

    private void startPunishmentExpiryTask(){
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            BotMain bot = discordBot;
            if(bot == null || bot.getJda() == null || bot.getDiscordServer() == null) return;

            try{
                for(UUID uuid : playerPunishmentsCache.getMainCache().keySet()){
                    CopyOnWriteArrayList<Punishment> playerPunishments = playerPunishmentsCache.getMainCache().get(uuid);
                    if(playerPunishments == null || !playerHasActivePunishments(uuid)) continue;

                    for(Punishment p : playerPunishments){
                        if(p.expiresAt() <= System.currentTimeMillis() && p.active() && p.expiresAt() != 0){
                            String sql = "UPDATE punishments SET active = false WHERE id = ?";
                            try(Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
                                ps.setString(1, p.id());
                                ps.executeUpdate();
                            }
                            playerPunishmentsCache.expirePunishmentById(uuid, p.id());

                            PunishmentType type = p.punishmentType();
                            PunishmentScopes scope = p.scope();

                            UUID targetUUID = p.uuid();
                            String userId = getPlayerVerificationCache().getDiscordIdFromUuid(targetUUID);

                            if((scope == PunishmentScopes.DISCORD || scope == PunishmentScopes.GLOBAL) && userId != null){
                                bot.getJda().retrieveUserById(userId).queue(targetUser -> {
                                    Guild dcServer = bot.getDiscordServer();
                                    if(type == PunishmentType.TEMP_BAN){
                                        long bannedRoleID = botConfig.getConfig().getLong("ban-role-id");
                                        long verifiedRoleID = botConfig.getConfig().getLong("verification.verified-role-id");
                                        Role bannedRole = dcServer.getRoleById(bannedRoleID);
                                        dcServer.retrieveMemberById(userId).queue(member -> {
                                            if(member.getRoles().contains(bannedRole)){
                                                dcServer.removeRoleFromMember(member, bannedRole).queue();
                                                dcServer.addRoleToMember(member, dcServer.getRoleById(verifiedRoleID)).queue();
                                            }
                                        });
                                    }
                                    if(type == PunishmentType.TEMP_MUTE){
                                        dcServer.retrieveMemberById(userId).queue(targetMember -> {
                                            long timeoutRoleID = botConfig.getConfig().getLong("timeout-role-id");
                                            Role timeoutRole = dcServer.getRoleById(timeoutRoleID);

                                            if(targetMember.getRoles().contains(timeoutRole)) dcServer.removeRoleFromMember(targetMember, timeoutRole).queue();
                                        });

                                        dcServer.removeTimeout(targetUser).queue();
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (Exception e){
                getLogger().severe("Punishment expiry task failed: " + e.getMessage());
            }
        }, 0L, 20L * 5);
    }

    private void disableOnMainThread(){
        if(!isEnabled()) return;
        if(Bukkit.isPrimaryThread()) getServer().getPluginManager().disablePlugin(this);
        else Bukkit.getScheduler().runTask(this, () -> {
            if(isEnabled()) getServer().getPluginManager().disablePlugin(this);
        });
    }

    private void sendEmbedInBannedChannel(){
        //Sending the embed in the banUsersChannel.
        String banUsersChannelIDs = botConfig.getConfig().getString("punished-users-channel.id");
        try{
            long banUsersChannelID = Long.parseLong(banUsersChannelIDs);
            TextChannel bannedUsersChannel = discordBot.getJda().getTextChannelById(banUsersChannelID);

            bannedUsersChannel.getHistory().retrievePast(1).queue(messages -> {
                if(messages.isEmpty()){
                    //Getting the color
                    int redValue = botConfig.getConfig().getInt("punished-users-channel.embed-color.RED");
                    int greenValue = botConfig.getConfig().getInt("punished-users-channel.embed-color.GREEN");
                    int blueValue = botConfig.getConfig().getInt("punished-users-channel.embed-color.BLUE");
                    Color embedColor = Color.fromRGB(redValue, greenValue, blueValue);

                    //Getting the title and description
                    String embedTitle = botConfig.getConfig().getString("punished-users-channel.embed-title");
                    String description = botConfig.getConfig().getString("punished-users-channel.embed-description");
                    bannedUsersChannel.sendMessageEmbeds(getEmbedBuilder(embedColor, embedTitle, description).build()).addComponents(ActionRow.of(Button.primary("getid", "Get Your Punishment ID"))).queue();
                }
            });
        } catch(Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] There was a problem sending the embeds in the 'punished-users-channel (botconfig.yml)'. See message:");
            Bukkit.getLogger().warning(e.getMessage());
        }
    }

    private void sendEmbedInVerificationChannel(){
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
            }
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
        BotMain bot = discordBot;
        discordBot = null;
        if(bot != null && bot.getJda() != null){
            bot.getJda().shutdownNow();
            getLogger().info("Bot shut down successfully!");
        }

        DatabaseManager database = databaseManager;
        if(database != null) database.closeDataSource();

        getLogger().info("DiscordUtils has been disabled successfully!");
    }

    //Handles the chat input for different configurations of the plugin
    @EventHandler
    public void chatAsync(AsyncPlayerChatEvent event){
        UUID playerUUID = event.getPlayer().getUniqueId();
        Consumer<String> callback = chatInput.remove(playerUUID);
        if(callback == null) return;

        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(this, () -> callback.accept(message));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        //If the code expired, sends him a message to inform him
        if(getPlayerVerificationCache().isCodeExpired(player.getUniqueId())){
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Sound sound = Sounds.fromKey(getConfig().getString("code-expired-sound", "block.note_block.bass"));
                float volume = getConfig().getInt("ces-volume");
                float pitch = getConfig().getInt("ces-pitch");

                String message = getConfig().getString("code-expired-message", "&cVerification code expired. Please run &l/verify &cagain.");
                message = PlaceholderAPI.setPlaceholders(player, message);

                getPlayerVerificationCache().removeFromDiscord(getPlayerVerificationCache().getCode(player.getUniqueId()));
                getPlayerVerificationCache().deleteCode(player.getUniqueId());

                player.sendMessage(Texts.color(message));
                player.playSound(player.getLocation(), sound, volume, pitch);
            }, 40L);
        }

        //Checking if the player is still verifying, in order to give him the code again.
        if(getPlayerVerificationCache().isPlayerVerifying(player.getUniqueId())){
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Sound sound = Sounds.fromKey(getConfig().getString("give-verification-code-sound", "block.note_block.pling"));
                float volume = getConfig().getInt("gvcs-volume", 1);
                float pitch = getConfig().getInt("gvcs-pitch", 1);
                List<String> rawMessage = getConfig().getStringList("resend-verification-code-message");

                for(String line : rawMessage){
                    line = line.replace("%code%", getPlayerVerificationCache().getCode(player.getUniqueId()));
                    line = PlaceholderAPI.setPlaceholders(player, line);
                    player.sendMessage(Texts.color(line));
                }
                player.playSound(player.getLocation(), sound, volume, pitch);
            }, 40L);
        }
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

    //Method for getting a list of all Punishments
    private CopyOnWriteArrayList<Punishment> getAllPunishments(UUID playerUUID) throws SQLException{
        CopyOnWriteArrayList<Punishment> allPunishments = new CopyOnWriteArrayList<>();
        String sql = "SELECT * FROM punishments WHERE uuid = ?";

        try(Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, playerUUID.toString());

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

        Sound broadcastSound = Sounds.fromKey(getConfig().getString("boardcast-message-sound"));

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

    private boolean playerHasActivePunishments(UUID playerUUID){
        List<Punishment> punishments = playerPunishmentsCache.getMainCache().get(playerUUID);
        return punishments != null && punishments.stream().anyMatch(Punishment::active);
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
    public Map<UUID, AddingState> getPunishmentsAddingStates(){
        return punishmentsAddingStates;
    }

    public BotMain getDiscordBot() {
        return discordBot;
    }

    public PlayerPunishmentDataCache getPlayerPunishmentDataCache(){
        return playerPunishmentsCache;
    }
    public PlayerVerificationCache getPlayerVerificationCache(){
        return playerVerificationCache;
    }
}
