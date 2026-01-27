//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
This class helps with applying the punishment in game/in discord server
and inserting them into the database.
 */
public class PunishmentContext {
    private final DiscordUtils plugin;
    private final AddingState state;
    private final OfflinePlayer staff;
    private final OfflinePlayer targetPlayer;

    private final FileConfiguration botConfig;

    public PunishmentContext(DiscordUtils plugin, OfflinePlayer staff, AddingState state) throws SQLException {
        this.plugin = plugin;
        this.state = state;
        this.staff = staff;

        //Getting the target player
        this.targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);
        this.botConfig = plugin.botFile().getConfig();
    }

    //Helper method to get the discordId of the target player. Useful for DISCORD/GLOBAL scope
    private String getTargetUserID(String ign) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String SQL = "SELECT discordId FROM playersVerification WHERE ign = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(SQL)){
            ps.setString(1, ign);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return null;

                return rs.getString("discordId");
            }
        }
    }

    private boolean isBotConfigured(){
        String guildId = plugin.botFile().getConfig().getString("guild-id");
        String botToken = plugin.botFile().getConfig().getString("bot-token");

        return guildId != null && botToken != null;
    }

    private EmbedBuilder getEmbedBuilder(User targetUser, String message, Guild dcServer, String title) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setColor(Color.RED.getRGB());
        if(state.type != PunishmentType.KICK && !state.type.name().contains("WARN")) embed.setFooter("ID: " + state.ID);

        String field = message
                .replace("%scope%", state.scope.name())
                .replace("%reason%", state.reason)
                .replace("%server_name%", dcServer.getName())
                .replace("%user%", targetUser.getName())
                .replace("%time_left%", plugin.formatTime(state.duration))
                .replace("%expiration_time%", getFormattedTime(System.currentTimeMillis() + state.duration));
        embed.setDescription(field);
        return embed;
    }

    //Method for inserting the punishment into the db
    private void insertPunishment(){
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String playerName = staff.getName();
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);

        try(PreparedStatement ps = dbConnection.prepareStatement("""
                INSERT INTO punishments (id, uuid, type, scope, reason, staff, created_at, expire_at, active, removed, removed_at, appeal_state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)){
            ps.setString(1, state.ID);
            ps.setString(2, targetPlayer.getUniqueId().toString());
            ps.setString(3, state.type.name());
            ps.setString(4, state.scope.name());
            ps.setString(5, state.reason);
            ps.setString(6, playerName);
            ps.setLong(7, System.currentTimeMillis());
            ps.setString(12, "x");

            //Handling various cases
            if(state.type == PunishmentType.KICK){
                ps.setLong(8, 0);
                ps.setBoolean(9, false);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            else if(state.type.toString().contains("WARN")){
                ps.setLong(8, 0);
                ps.setBoolean(9, true);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            else if(state.type.name().contains("TEMP")){
                ps.setLong(8, System.currentTimeMillis() + state.duration);
                ps.setBoolean(9, true);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            else if(state.type.toString().contains("PERM")){
                ps.setLong(8, 0);
                ps.setBoolean(9, true);
                ps.setBoolean(10, false);
                ps.setLong(11, 0);
            }
            ps.executeUpdate();
        } catch (SQLException e){ throw new RuntimeException(e); }
    }
    //Colored scope. For displaying the scope in MC
    private String getColoredStringScope(AddingState state) {
        return switch(state.scope){
            case DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
        };
    }
    //Formatting the time.
    private String getFormattedTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        return time.format(formatter);
    }

    //Helps with creating an ID for each punishment
    private String createId(){
        int idLength = plugin.getConfig().getInt("punishment-id-length");
        StringBuilder id = new StringBuilder(idLength);
        SecureRandom random = new SecureRandom();

        id.append("#");
        for(int i = 0; i < idLength; i++){
            id.append(random.nextInt(10)); //Generates everytime a number from 0-9
        }

        return id.toString();
    }

    //Kicks
    public void applyKick() throws SQLException {
        switch(state.scope){
            case MINECRAFT -> {kickMinecraft(); insertPunishment();}
            case DISCORD ->  {kickDiscord(); insertPunishment();}
            case GLOBAL ->{kickDiscord(); kickMinecraft(); insertPunishment();}
        }
    }
    private void kickMinecraft(){
       Bukkit.getScheduler().runTask(plugin, () -> {
           String kickMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.kick-message"));
           ((Player) targetPlayer).kickPlayer(kickMessage
                   .replace("%scope%", getColoredStringScope(state))
                   .replace("%reason%", state.reason)
           );
       });
    }
    private void kickDiscord() throws SQLException {
        Guild dcServer = plugin.getDiscordBot().getDiscordServer();

        //Attempts to send a DM to the target user about the kick
        plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(user -> {
            user.openPrivateChannel().queue(channel -> {
                String message = botConfig.getString("user-punishments-messages.kick-message");
                EmbedBuilder embed = getEmbedBuilder(user, message, dcServer, "KICK");

                channel.sendMessageEmbeds(embed.build()).queue(
                        success -> dcServer.kick(user).reason(state.reason).queue(),
                        failure -> dcServer.kick(user).reason(state.reason).queue()
                );
            }, failure -> dcServer.kick(user).reason(state.reason).queue());
        });
    }

    //Permanent Bans
    public void applyPermBan() throws SQLException {
        state.ID = createId();

        switch(state.scope){
            case MINECRAFT ->{
                permBanMC();
                insertPunishment();
            }
            case DISCORD -> {
                permBanDC();
                insertPunishment();

                //Giving the Banned Role to the user
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(user -> {
                    long bannedRoleID = botConfig.getLong("ban-role-id");
                    Role bannedRole = plugin.getDiscordBot().getDiscordServer().getRoleById(bannedRoleID);
                    plugin.getDiscordBot().getDiscordServer().addRoleToMember(user, bannedRole).queue();
                });
            }
            case GLOBAL -> {
                permBanDC();
                permBanMC();
                insertPunishment();

                //Giving the Banned Role to the user
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(user -> {
                    long bannedRoleID = botConfig.getLong("ban-role-id");
                    Role bannedRole = plugin.getDiscordBot().getDiscordServer().getRoleById(bannedRoleID);
                    plugin.getDiscordBot().getDiscordServer().addRoleToMember(user, bannedRole).queue();
                });
            }
        }
    }
    private void permBanMC(){
        //Kicks the target player if he is online, then marks him as banned
        if(targetPlayer.isOnline()){
            Bukkit.getScheduler().runTask(plugin, () -> {
                String permBanMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.perm-ban-message"));
                ((Player) targetPlayer).kickPlayer(permBanMessage
                        .replace("%scope%", getColoredStringScope(state))
                        .replace("%reason%", state.reason)
                        .replace("%id%", state.ID)
                );
            });
        }
    }
    private void permBanDC() throws SQLException {
        Guild dcServer = plugin.getDiscordBot().getDiscordServer();

        //Attempts to send a DM to the target user about the perm ban
        plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
            targetUser.openPrivateChannel().queue(privateChannel -> {
                String message = botConfig.getString("user-punishments-messages.permanent-ban-message");

                EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "PERMANENT BAN");

                privateChannel.sendMessageEmbeds(embed.build()).queue();
            });
        });
    }

    //Permanent Ban Warnings
    public void applyPermBanWarn() throws SQLException {
        state.ID = createId();

        //Inserts the warning, then checks if the target player reached the number of warns.
        insertPunishment();
        if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.PERM_BAN_WARN, state.scope)){
            //Expires the warns
            plugin.getDatabaseManager().expireAllWarns(targetPlayer, PunishmentType.PERM_BAN_WARN, state.scope);

            state.type = PunishmentType.PERM_BAN;
            applyPermBan();
        }
        else{ //If he didn't reach the nr of warns, sends him messages.
            //If the target player is online, sends him a chat message
            if(targetPlayer.isOnline()){
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-ban-warn-message");
                    for(String line : message){
                        String coloredLine =  ChatColor.translateAlternateColorCodes('&', line
                                .replace("%scope%", getColoredStringScope(state))
                                .replace("%reason%", state.reason)
                        );
                        ((Player) targetPlayer).sendMessage(coloredLine);
                    }
                });
            }

            //Attempts to send the target user a DM about the warning (if the bot is configured)
            if(isBotConfigured()){
                Guild dcServer = plugin.getDiscordBot().getDiscordServer();
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
                    targetUser.openPrivateChannel().queue(privateChannel -> {
                        String message = botConfig.getString("user-punishments-messages.permanent-ban-warn-message");
                        EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "PERMANENT BAN WARNING");

                        privateChannel.sendMessageEmbeds(embed.build()).queue();
                    });
                });
            }
        }
    }

    //Temporary Bans
    public void applyTempBan() throws SQLException {
        state.ID = createId();

        switch(state.scope){
            case MINECRAFT -> {
                tempBanMC();
                insertPunishment();
            }
            case DISCORD -> {
                tempBanDC();
                insertPunishment();

                //Giving the Banned Role to the user
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(user -> {
                    long bannedRoleID = botConfig.getLong("ban-role-id");
                    Role bannedRole = plugin.getDiscordBot().getDiscordServer().getRoleById(bannedRoleID);
                    plugin.getDiscordBot().getDiscordServer().addRoleToMember(user, bannedRole).queue();
                });
            }
            case GLOBAL -> {
                tempBanDC();
                tempBanMC();
                insertPunishment();

                //Giving the Banned Role to the user
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(user -> {
                    long bannedRoleID = botConfig.getLong("ban-role-id");
                    Role bannedRole = plugin.getDiscordBot().getDiscordServer().getRoleById(bannedRoleID);
                    plugin.getDiscordBot().getDiscordServer().addRoleToMember(user, bannedRole).queue();
                });
            }
        }
    }
    private void tempBanMC(){
        //Kick the target player if he is online
        if(targetPlayer.isOnline()){
            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.temp-ban-message"));
                ((Player) targetPlayer).kickPlayer(message
                        .replace("%scope%", getColoredStringScope(state))
                        .replace("%reason%", state.reason)
                        .replace("%id%", state.ID)
                        .replace("%time_left%", plugin.formatTime(state.duration))
                        .replace("%expiration_time%", getFormattedTime(System.currentTimeMillis() + state.duration))
                );
            });
        }
    }
    private void tempBanDC() throws SQLException {
        Guild dcServer = plugin.getDiscordBot().getDiscordServer();

        //Attempts to send the target user a DM
        plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
            targetUser.openPrivateChannel().queue(privateChannel -> {
                String message = botConfig.getString("user-punishments-messages.temporary-ban-message");
                EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "TEMPORARY BAN");
                privateChannel.sendMessageEmbeds(embed.build()).queue();
            });
        });
    }

    //Temporary Ban Warnings
    public void applyTempBanWarn() throws SQLException {
        state.ID = createId();

        //Inserts the warning
        insertPunishment();

        //Checks if the target player/user has reached the nr of warns
        if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.TEMP_BAN_WARN, state.scope)){
            //Expires the warns
            plugin.getDatabaseManager().expireAllWarns(targetPlayer,  PunishmentType.TEMP_BAN_WARN, state.scope);

            //Applies the temporary ban
            state.type = PunishmentType.TEMP_BAN;
            applyTempBan();
        }
        //If he didn't, sends him some messages
        else{
            //If the target player is online in game, sends a chat message
            if(targetPlayer.isOnline()){
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<String> message =  plugin.getConfig().getStringList("player-punishments-messages.temp-ban-warn-message");
                    for(String line : message){
                        String coloredLine = ChatColor.translateAlternateColorCodes('&', line
                                .replace("%scope%", getColoredStringScope(state))
                                .replace("%reason%", state.reason)
                        );
                        ((Player) targetPlayer).sendMessage(coloredLine);
                    }
                });
            }

            //Attempts to send the target user a DM (if the bot is configured)
            if(isBotConfigured()){
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
                    Guild dcServer = plugin.getDiscordBot().getDiscordServer();
                    targetUser.openPrivateChannel().queue(privateChannel -> {
                        String message = botConfig.getString("user-punishments-messages.temporary-ban-warn-message");
                        EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "TEMPORARY BAN WARNING");
                        privateChannel.sendMessageEmbeds(embed.build()).queue();
                    });
                });
            }
        }
    }

    //Permanent Mutes/Timeouts
    public void applyPermMuteTimeout() throws SQLException{
        state.ID = createId();

        switch(state.scope){
            case MINECRAFT -> {permMuteMC(); insertPunishment();}
            case DISCORD -> {permTimeoutDC(); insertPunishment();}
            case GLOBAL -> {permTimeoutDC(); permMuteMC(); insertPunishment();}
        }
    }
    private void permMuteMC(){
        //If the target player is online, sends him a chat message
        if(targetPlayer.isOnline()){
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-mute-message");
                for(String line : message){
                    ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', line
                            .replace("%scope%", getColoredStringScope(state))
                            .replace("%reason%", state.reason)
                            .replace("%id%", state.ID)
                    ));
                }
            });
        }
    }
    private void permTimeoutDC() throws SQLException {
        Guild dcServer = plugin.getDiscordBot().getDiscordServer();

        //Attempts to send the target user a DM
        plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
            dcServer.retrieveMemberById(targetUser.getId()).queue(targetMember -> {
                //Gives the targetMember the timeout role
                long timeoutRoleId = botConfig.getLong("timeout-role-id");
                Role timeoutRole = dcServer.getRoleById(timeoutRoleId);
                dcServer.addRoleToMember(targetMember, timeoutRole).queue();

                targetUser.openPrivateChannel().queue(privateChannel -> {
                    String message = botConfig.getString("user-punishments-messages.permanent-timeout-message");
                    EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "PERMANENT TIMEOUT");
                    privateChannel.sendMessageEmbeds(embed.build()).queue();
                });
            });
        });
    }

    //Permanent Mute/Timeout warnings
    public void applyPermMuteTimeoutWarn() throws SQLException {
        state.ID = createId();

        //Inserts the warning
        insertPunishment();

        //Checks if the target user/player has reached the nr of warns
        if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.PERM_MUTE_WARN, state.scope)){
            //Expires the warnings
            plugin.getDatabaseManager().expireAllWarns(targetPlayer,   PunishmentType.PERM_MUTE_WARN, state.scope);

            //Applies the perm mute/timeout
            state.type = PunishmentType.PERM_MUTE;
            applyPermMuteTimeout();
        }
        //If not, sends messages
        else{
            //If the target player is online, in game, sends a chat message
            if(targetPlayer.isOnline()){
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<String> message =  plugin.getConfig().getStringList("player-punishments-messages.perm-mute-warn-message");
                    for(String line : message){
                        ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', line
                                .replace("%scope%", getColoredStringScope(state))
                                .replace("%reason%", state.reason)
                        ));
                    }
                });
            }

            //Attempts to send the target user a DM (if the bot is configured)
            if(isBotConfigured()){
                Guild dcServer = plugin.getDiscordBot().getDiscordServer();
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
                    targetUser.openPrivateChannel().queue(privateChannel -> {
                        String message = botConfig.getString("user-punishments-messages.permanent-timeout-warn-message");
                        EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "PERMANENT TIMEOUT/MUTE WARNING");
                        privateChannel.sendMessageEmbeds(embed.build()).queue();
                    });
                });
            }
        }
    }

    //Temporary Mutes/Timeouts
    public void applyTempMuteTimeout() throws SQLException {
        state.ID = createId();

        switch(state.scope){
            case MINECRAFT -> {tempMuteMC(); insertPunishment();}
            case DISCORD -> {tempTimeoutDC(); insertPunishment();}
            case GLOBAL -> {tempMuteMC(); tempTimeoutDC(); insertPunishment();}
        }
    }
    private void tempMuteMC(){
        //If the target player is online on the game, sends a chat message
        if(targetPlayer.isOnline()){
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-message");
                for(String line : message){
                    ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', line
                            .replace("%scope%", getColoredStringScope(state))
                            .replace("%reason%", state.reason)
                            .replace("%id%", state.ID)
                            .replace("%time_left%", plugin.formatTime(state.duration))
                            .replace("%expiration_time%", getFormattedTime(System.currentTimeMillis() + state.duration))
                    ));
                }
            });
        }
    }
    private void tempTimeoutDC() throws SQLException {
        //Attempts to send a DM to the target user
        Guild dcServer = plugin.getDiscordBot().getDiscordServer();
        plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
            dcServer.retrieveMemberById(targetUser.getId()).queue(targetMember -> {
                //Gives the targetMember the timeout role
                long timeoutRoleId = botConfig.getLong("timeout-role-id");
                Role timeoutRole = dcServer.getRoleById(timeoutRoleId);
                dcServer.addRoleToMember(targetMember, timeoutRole).queue();

                targetUser.openPrivateChannel().queue(privateChannel -> {
                    String message = botConfig.getString("user-punishments-messages.temporary-timeout-message");
                    EmbedBuilder embed =  getEmbedBuilder(targetUser, message, dcServer, "TEMPORARY TIMEOUT");
                    privateChannel.sendMessageEmbeds(embed.build()).queue(
                            success -> targetMember.timeoutFor(state.duration, TimeUnit.MILLISECONDS).reason(state.reason).queue(),
                            failure -> targetMember.timeoutFor(state.duration, TimeUnit.MILLISECONDS).reason(state.reason).queue()
                    );
                }, failure -> targetMember.timeoutFor(state.duration, TimeUnit.MILLISECONDS).reason(state.reason).queue());
            });
        });
    }

    //Temporary Mutes/Timeouts Warnings
    public void applyTempMuteTimeoutWarn() throws SQLException {
        state.ID = createId();

        //Inserts the warning
        insertPunishment();

        //Checks if the target player/user has reached the number of warns
        if(plugin.getDatabaseManager().playerHasTheNrOfWarns(targetPlayer, PunishmentType.TEMP_MUTE_WARN, state.scope)){
            //Expire the warnings
            plugin.getDatabaseManager().expireAllWarns(targetPlayer, PunishmentType.TEMP_MUTE_WARN, state.scope);

            //Applies the temporary mute/timeout
            state.type = PunishmentType.TEMP_MUTE;
            applyTempMuteTimeout();
        }
        //If not, sends messages to the user/player about the warning
        else{
            //Sends a chat message if the target player is online
            if(targetPlayer.isOnline()){
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-warn-message");
                    for(String line : message){
                        ((Player) targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', line
                                .replace("%scope%", getColoredStringScope(state))
                                .replace("%reason%", state.reason)
                        ));
                    }
                });
            }

            //Attempts to send the target user a DM (if the bot is configured)
            if(isBotConfigured()){
                Guild dcServer = plugin.getDiscordBot().getDiscordServer();
                plugin.getDiscordBot().getJda().retrieveUserById(getTargetUserID(targetPlayer.getName())).queue(targetUser -> {
                    targetUser.openPrivateChannel().queue(privateChannel -> {
                        String message = botConfig.getString("user-punishments-messages.temporary-timeout-warn-message");
                        EmbedBuilder embed = getEmbedBuilder(targetUser, message, dcServer, "TEMPORARY TIMEOUT WARNING");
                        privateChannel.sendMessageEmbeds(embed.build()).queue();
                    });
                });
            }
        }
    }
}
