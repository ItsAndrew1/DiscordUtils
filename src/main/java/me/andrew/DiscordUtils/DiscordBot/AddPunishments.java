package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddPunishments extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final BotMain bot;
    private final Map<Long, AddingState> addingStateMap = new HashMap<>();

    public AddPunishments(DiscordUtils plugin, BotMain bot) {
        this.plugin = plugin;
        this.bot = bot;

        //Task for auto deleting users from the map
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            addingStateMap.entrySet().removeIf(entry -> now - entry.getValue().lastInteraction > TimeUnit.MINUTES.toMillis(2));
        }, 0L, 20L*60); //Task runs every minute
    }

    public void punishPlayer(SlashCommandInteractionEvent event, OfflinePlayer targetPlayer) throws SQLException {
        //Creating the state
        AddingState newState = new AddingState(
                createId(),
                targetPlayer,
                getStaffName(event.getUser().getId()),
                null,
                null,
                null,
                0,
                System.currentTimeMillis()
        );

        //Attaching each user the state (into a map)
        addingStateMap.put(event.getUser().getIdLong(), newState);

        //Menu for punishment type
        StringSelectMenu setTypeMenu = StringSelectMenu.create("punishment:type")
                .setPlaceholder("...")
                .setRequiredRange(1, 1)
                .addOption("Permanent Ban", "PERM_BAN")
                .addOption("Temporary Ban", "TEMP_BAN")
                .addOption("Permanent Ban Warning", "PERM_BAN_WARN")
                .addOption("Temporary Ban Warning", "TEMP_BAN_WARN")
                .addOption("Kick", "KICK")
                .addOption("Permanent Mute", "PERM_MUTE")
                .addOption("Temporary Mute", "TEMP_MUTE")
                .addOption("Permanent Mute Warning", "PERM_MUTE_WARN")
                .addOption("Temporary Mute Warning", "TEMP_MUTE_WARN")
                .build();

        event.reply("Choose punishment type: ").setEphemeral(true).addComponents(ActionRow.of(setTypeMenu)).queue();
    }

    private String getStaffName(String userId) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        String sql = "SELECT uuid FROM playersVerification WHERE discordId = ?";
        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, userId);
            try(ResultSet rs = ps.executeQuery()){
                Player staff = Bukkit.getPlayer(UUID.fromString(rs.getString("uuid")));
                return staff.getName();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event){
        long userId = event.getUser().getIdLong();
        AddingState state = addingStateMap.get(userId);

        FileConfiguration botConfig = plugin.botFile().getConfig();

        if(event.getComponentId().equals("punishment:type")){
            //Checking if the state has not expired
            if(state == null){
                event.reply("This punishment session **has expired**. Please run */punish <ign>* again!").setEphemeral(true).queue();
                return;
            }

            //Setting the type
            state.type = PunishmentType.valueOf(event.getValues().getFirst());

            //Updating the last interaction
            state.lastInteraction = System.currentTimeMillis();

            //Building the Modal
            int minimumLength = botConfig.getInt("minimum-length-reason");
            int maximumLength = botConfig.getInt("maximum-length-reason");
            TextInput body = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Enter the reason for the punishment")
                    .setMinLength(minimumLength)
                    .setMaxLength(maximumLength)
                    .build();

            Modal reasonModal = Modal.create("psReason", "Reason of Punishment")
                    .addComponents(Label.of("Reason", body))
                    .build();
            event.replyModal(reasonModal).queue();
        }

        if(event.getComponentId().equals("punishment:scope")){
            //Check if the state has not expired
            if(state == null){
                event.reply("This punishment session **has expired**. Plesae run */punish <ign>* again!").setEphemeral(true).queue();
                return;
            }

            //Setting the punishment scope
            state.scope = PunishmentScopes.valueOf(event.getValues().getFirst());

            //Checking various cases if the user is already banned/muted
            try{
                if(state.scope == PunishmentScopes.DISCORD){
                    //Checking if the user/player is already banned in the dc server
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned on Discord!").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already banned globally
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned Globally.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already muted on discord
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted on Discord!").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already muted globally
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted Globally.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }
                }

                if(state.scope == PunishmentScopes.MINECRAFT){
                    //Checking if the user/player is already banned on Minecraft
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned on Minecraft.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already banned globally
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned Globally.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already muted on Minecraft
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted on Minecraft!").setEphemeral(true).queue();
                        return;
                    }

                    //Checking if the user/player is already muted Globally
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted Globally.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }
                }

                if(state.scope == PunishmentScopes.GLOBAL){
                    //Checking if the user/player is already muted on discord
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted on Discord. Use the *Minecraft Scope* instead.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already muted on Minecraft
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted on Minecraft. Use the *Discord Scope* instead.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already muted Globally
                    if(plugin.getDatabaseManager().isPlayerMuted(state.targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already muted Globally.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already banned on Discord
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned on Discord. Use the *Minecraft Scope* instead.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already banned on Minecraft
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned on Minecraft. Use the *Discord Scope* instead.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }

                    //Checking if the user/player is already banned Globally
                    if(plugin.getDatabaseManager().isPlayerBanned(state.targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                        event.reply("Player **\\"+state.targetPlayer.getName()+"** is already banned Globally.").setEphemeral(true).queue();
                        addingStateMap.remove(event.getUser().getIdLong());
                        return;
                    }
                }
            } catch (SQLException e){
                throw new RuntimeException(e);
            }

            //Updating the last interaction
            state.lastInteraction = System.currentTimeMillis();

            //If the punishment is a temp mute or temp ban, opens the duration modal. Else, inserts the punishment.
            if(isTemporary(state.type)){
                int minimumLength = botConfig.getInt("minimum-length-duration");
                int maximumLength = botConfig.getInt("maximum-length-duration");
                TextInput body = TextInput.create("duration", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Enter the duration of the punishment (eg. 2d5h10m15s)")
                        .setMinLength(minimumLength)
                        .setMaxLength(maximumLength)
                        .build();

                Modal durationModal = Modal.create("psDuration", "Duration of Punishment")
                        .addComponents(Label.of("Duration", body))
                        .build();
                event.replyModal(durationModal).queue();
            }
            else{
                try {
                    final Guild dcServer = bot.getDiscordServer();
                    final User targetUser = (User) User.fromId(getTargetUserID(state.targetPlayer.getName()));
                    final Member targetMember = dcServer.getMember(targetUser);

                    //Checking each type of punishment to apply them in game/in discord
                    //If the punishment is a kick
                    if(state.type == PunishmentType.KICK){
                        String messageMC = plugin.getConfig().getString("player-punishments-messages.kick-message");

                        //Handles each scope
                        switch(state.scope){
                            case MINECRAFT:
                                //Check if the player is online
                                if(!state.targetPlayer.isOnline()){
                                    event.reply("Player **\\"+state.targetPlayer.getName()+"** is not online at the moment!").setEphemeral(true).queue();
                                    return;
                                }

                                ((Player) state.targetPlayer).kickPlayer(messageMC
                                        .replace("%reason%", state.reason)
                                        .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(getPlayerStaff(event.getUser().getId())))
                                );
                                break;

                            case DISCORD:
                                String messageDC1 = botConfig.getString("user-punishments-messages.kick-message");

                                targetUser.openPrivateChannel().queue(channel -> {
                                    //Sends the user a DM
                                    channel.sendMessage(messageDC1
                                            .replace("%user%", targetUser.getName())
                                            .replace("%reason%", state.reason)
                                            .replace("%scope%", getPunishmentTypeString(state.type))
                                            .replace("%server_name%", dcServer.getName())
                                    ).queue(success -> {
                                        //Kicks the user from the discord server.
                                        dcServer.kick(targetUser)
                                                .reason(state.reason)
                                                .queue();
                                    }, failure -> {
                                        //If the DM failed to send, it still kicks the user
                                        dcServer.kick(targetUser)
                                                .reason(state.reason)
                                                .queue();
                                    });
                                }, failure -> {
                                    //If the DM couldn't be opened, still kicks the user
                                    dcServer.kick(targetUser)
                                            .reason(state.reason)
                                            .queue();
                                });
                                break;

                            case GLOBAL:
                                //If the target player isn't online, returns with a message
                                if(!state.targetPlayer.isOnline()){
                                    event.reply("Player **\\"+state.targetPlayer.getName()+"** is not online on the *Minecraft Server* at the moment.\nUse the **Discord Scope** instead!").setEphemeral(true).queue();
                                    return;
                                }

                                //Kicks the player from the mc server
                                ((Player) state.targetPlayer).kickPlayer(messageMC
                                        .replace("%reason%", state.reason)
                                        .replace("%scope%", plugin.getChoosePunishScopeGUI().getStringScope(getPlayerStaff(event.getUser().getId())))
                                );

                                //And kicks them from the discord server
                                String messageDC2 = botConfig.getString("user-punishments-messages.kick-message");
                                targetUser.openPrivateChannel().queue(channel -> {
                                    //Sends the user a DM
                                    channel.sendMessage(messageDC2
                                            .replace("%user%", targetUser.getName())
                                            .replace("%reason%", state.reason)
                                            .replace("%scope%", getPunishmentTypeString(state.type))
                                            .replace("%server_name%", dcServer.getName())
                                    ).queue(success -> {
                                        //Kicks the user from the discord server.
                                        dcServer.kick(targetUser)
                                                .reason(state.reason)
                                                .queue();
                                    }, failure -> {
                                        //If the DM failed to send, it still kicks the user
                                        dcServer.kick(targetUser)
                                                .reason(state.reason)
                                                .queue();
                                    });
                                }, failure -> {
                                    //If the DM couldn't be opened, still kicks the user
                                    dcServer.kick(targetUser)
                                            .reason(state.reason)
                                            .queue();
                                });
                                break;
                        }
                    }

                    //If the punishment is a permanent ban
                    if(state.type == PunishmentType.PERM_BAN){
                        switch(state.scope){
                            case MINECRAFT:
                                //Kicks the player with the ban message if the player is online
                                String banMessageMC1 = plugin.getConfig().getString("player-punishments-messages.perm-ban-message");
                                if(state.targetPlayer.isOnline()){
                                    ((Player) state.targetPlayer).kickPlayer(banMessageMC1
                                            .replace("%reason%", state.reason)
                                            .replace("%scope%", getPunishmentColoredScope(state.scope))
                                            .replace("%id%", state.ID)
                                    );
                                }
                                break;

                            case DISCORD:
                                //Bans the user
                                String banMessageDC1 = botConfig.getString("user-punishments-messages.perm-ban-message");

                                targetUser.openPrivateChannel().queue(channel -> {
                                    channel.sendMessage(banMessageDC1
                                            .replace("%reason%", state.reason)
                                            .replace("%scope%", getPunishmentNormalScope(state.scope))
                                    ).queue(success -> {
                                        dcServer.ban(targetUser, 0, TimeUnit.SECONDS).reason(state.reason).queue();
                                    }, failure -> {
                                        //If it fails to send the DM, still bans the user.
                                        dcServer.ban(targetUser, 0, TimeUnit.SECONDS).reason(state.reason).queue();
                                    });
                                }, failure -> {
                                    //If it fails to open the DM, still bans the user
                                    dcServer.ban(targetUser, 0, TimeUnit.SECONDS).reason(state.reason).queue();
                                });
                                break;

                            case GLOBAL:
                                //Kicks the player from the MC server if the player is online
                                String banMessageMC2 = plugin.getConfig().getString("player-punishments-messages.perm-ban-message");
                                if(state.targetPlayer.isOnline()){
                                    ((Player) state.targetPlayer).kickPlayer(banMessageMC2
                                            .replace("%reason%", state.reason)
                                            .replace("%scope%", getPunishmentColoredScope(state.scope))
                                            .replace("%id%", state.ID)
                                    );
                                }

                                //Bans the user from the DC server
                                String banMessageDC2 = botConfig.getString("user-punishments-messages.perm-ban-message");

                                targetUser.openPrivateChannel().queue(channel -> {
                                    channel.sendMessage(banMessageDC2
                                            .replace("%reason%", state.reason)
                                            .replace("%scope%", getPunishmentNormalScope(state.scope))
                                    ).queue(success -> {
                                        dcServer.ban(targetUser, 0, TimeUnit.SECONDS).reason(state.reason).queue();
                                    }, failure -> {
                                        //If it fails to send the DM, still bans the user.
                                        dcServer.ban(targetUser, 0, TimeUnit.SECONDS).reason(state.reason).queue();
                                    });
                                }, failure -> {
                                    //If it fails to open the DM, still bans the user
                                    dcServer.ban(targetUser, 0, TimeUnit.SECONDS).reason(state.reason).queue();
                                });
                                break;
                        }

                        //If the punishment is a permanent mute
                        if(state.type == PunishmentType.PERM_MUTE){
                            switch(state.scope){
                                case MINECRAFT:
                                    //Sends the player a chat message if the player is online
                                    if(state.targetPlayer.isOnline()){
                                        String chatMessage1 = plugin.getConfig().getString("player-punishments-messages.perm-mute-message");
                                        ((Player) state.targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessage1
                                                .replace("%reason%", state.reason)
                                                .replace("%scope%", getPunishmentColoredScope(state.scope))
                                                .replace("%id%", state.ID)
                                        ));
                                    }
                                    break;

                                case DISCORD:
                                    //Sends the user a DM message and timeouts the user.
                                    String dcMessage1 = botConfig.getString("user-punishments-messages.perm-mute-message");

                                    targetUser.openPrivateChannel().queue(channel -> {
                                        channel.sendMessage(dcMessage1
                                                .replace("%reason%", state.reason)
                                                .replace("%scope%", getPunishmentNormalScope(state.scope))
                                                .replace("%id%", state.ID)
                                                .replace("%server_name%", dcServer.getName())
                                                .replace("%user%", targetUser.getName())
                                        ).queue(success -> {
                                            targetMember.timeoutUntil(OffsetDateTime.now().plusYears(100)).reason(state.reason).queue();
                                        }, failure -> {
                                            //If it fails to send the message, it still timeouts the user
                                            targetMember.timeoutUntil(OffsetDateTime.now().plusYears(100)).reason(state.reason).queue(); //100 years should be enough to be permanent :))
                                        });
                                    }, failure -> {
                                        //If it fails to open the DM, still timeouts the user
                                        targetMember.timeoutUntil(OffsetDateTime.now().plusYears(100)).reason(state.reason).queue();
                                    });
                                    break;

                                case GLOBAL:
                                    //Sends the player a chat message if the player is online
                                    if(state.targetPlayer.isOnline()){
                                        String chatMessage2 =  plugin.getConfig().getString("player-punishments-messages.perm-mute-message");
                                        ((Player) state.targetPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessage2
                                                .replace("%reason%", state.reason)
                                                .replace("%scope%", getPunishmentColoredScope(state.scope))
                                                .replace("%id%", state.ID)
                                        ));
                                    }

                                    //Timeouts the user for 100 years :) (Permanent)
                                    String dcMessage2 = botConfig.getString("user-punishments-messages.perm-mute-message");

                                    targetUser.openPrivateChannel().queue(channel -> {
                                        channel.sendMessage(dcMessage2
                                                .replace("%reason%", state.reason)
                                                .replace("%scope%", getPunishmentNormalScope(state.scope))
                                                .replace("%id%", state.ID)
                                                .replace("%server_name%", dcServer.getName())
                                                .replace("%user%", targetUser.getName())
                                        ).queue(success -> {
                                            targetMember.timeoutUntil(OffsetDateTime.now().plusYears(100)).reason(state.reason).queue();
                                        }, failure -> {
                                            //If it fails to send the message, still timeouts the user
                                            targetMember.timeoutUntil(OffsetDateTime.now().plusYears(100)).reason(state.reason).queue();
                                        });
                                    }, failure -> {
                                        //If it fails to open the DM, still timeouts the user
                                        targetMember.timeoutUntil(OffsetDateTime.now().plusYears(100)).reason(state.reason).queue();
                                    });
                                    break;
                            }
                        }
                    }

                    event.reply("Punishment **"+getPunishmentTypeString(state.type)+"** for player *"+state.targetPlayer.getName()+"* applied successfully!").setEphemeral(true).queue();
                    insertPunishment(state);
                    addingStateMap.remove(userId);
                } catch (SQLException e) {
                    event.reply("There was a problem applying the punishment. "+e.getMessage()).setEphemeral(true).queue();
                    addingStateMap.remove(userId);
                }
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event){
        long userId =  event.getUser().getIdLong();
        AddingState state = addingStateMap.get(userId);

        if(event.getCustomId().equals("psReason")){
            //Check if the state has not expired
            if(state == null){
                event.reply("This punishment session **has expired**! Run */punish <ign>* again!").setEphemeral(true).queue();
                return;
            }

            //Setting the reason
            ModalMapping bodyInput = event.getValue("reason");
            state.reason = bodyInput.getAsString();

            //Updating the last interaction
            state.lastInteraction = System.currentTimeMillis();

            StringSelectMenu scopeMenu = StringSelectMenu.create("punishment:scope")
                    .setPlaceholder("...")
                    .setRequiredRange(1, 1)
                    .addOption("Minecraft Scope", "MINECRAFT")
                    .addOption("Discord Scope", "DISCORD")
                    .addOption("Global Scope", "GLOBAL")
                    .build();
            event.editMessage("Choose punishment scope: ").setComponents(ActionRow.of(scopeMenu)).queue();
        }

        if(event.getCustomId().equals("psDuration")){
            //Check if the state has not expired
            if(state == null){
                event.reply("This punishment session **has expired**! Run */punish <ign>* again!").setEphemeral(true).queue();
                return;
            }

            //Getting the duration
            ModalMapping bodyInput = event.getValue("duration");
            String durationString =  bodyInput.getAsString();
            state.expiresAt = parseDuration(durationString);

            //Inserts the punishment into the database
            try {

                insertPunishment(state);
                event.reply("Punishment **"+getPunishmentTypeString(state.type)+"** for player *\\"+state.targetPlayer.getName()+"* applied successfully!").setEphemeral(true).queue();

                addingStateMap.remove(userId);
            } catch (SQLException e) {
                event.reply("There was a problem applying the punishment.\n **"+e.getMessage()+"**").setEphemeral(true).queue();
                addingStateMap.remove(userId);
            }
        }
    }

    private boolean isTemporary(PunishmentType type){
        return type == PunishmentType.TEMP_BAN || type == PunishmentType.TEMP_MUTE;
    }

    private String getPunishmentTypeString(PunishmentType type){
        return switch(type){
            case PERM_BAN -> "PERMANENT BAN";
            case TEMP_BAN -> "TEMPORARY BAN";
            case PERM_MUTE -> "PERMANENT MUTE";
            case TEMP_MUTE -> "TEMP MUTE";
            case KICK -> "KICK";
            case PERM_BAN_WARN -> "PERMANENT BAN WARN";
            case TEMP_BAN_WARN -> "TEMPORARY BAN WARN";
            case PERM_MUTE_WARN -> "PERMANENT MUTE WARN";
            case TEMP_MUTE_WARN -> "TEMPORARY MUTE WARN";
        };
    }

    private Player getPlayerStaff(String userId) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT ign FROM playersVerification WHERE discordId = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, userId);
            try(ResultSet rs = ps.executeQuery()){
                String staffIgn = rs.getString("ign");
                return Bukkit.getPlayer(staffIgn);
            }
        }
    }

    private String getTargetUserID(String ign) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String SQL = "SELECT discordId FROM playersVerification WHERE ign = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(SQL)){
            ps.setString(1, ign);
            try(ResultSet rs = ps.executeQuery()){
                return rs.getString("discordId");
            }
        }
    }

    private String getPunishmentColoredScope(PunishmentScopes scope){
        return switch(scope){
            case MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
            case DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
        };
    }

    private String getPunishmentNormalScope(PunishmentScopes scope){
        return switch(scope){
            case MINECRAFT -> "MINECRAFT";
            case GLOBAL -> "GLOBAL";
            case DISCORD -> "DISCORD";
        };
    }

    private long parseDuration(String duration){
        long millis = 0;
        Matcher m = Pattern.compile("(\\d+)([dhms])").matcher(duration.toLowerCase());

        while(m.find()){
            int value = Integer.parseInt(m.group(1));
            switch(m.group(2)){
                case "d" -> millis += value*86400000L;
                case "h" -> millis += value*3600000L;
                case "m" -> millis += value*60000L;
                case "s" -> millis += value*1000L;
            }
        }
        return millis;
    }

    private void insertPunishment(AddingState state) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        //Data for the punishment
        OfflinePlayer targetPlayer = state.targetPlayer;
        String reason = state.reason;
        String staffName = state.staff;
        PunishmentType type = state.type;
        PunishmentScopes scope = state.scope;
        long expiresAt = state.expiresAt;

        try(PreparedStatement ps = dbConnection.prepareStatement("""
                INSERT INTO punishments (id, uuid, type, scope, reason, staff, created_at, expire_at, active, removed, removed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)){
            ps.setString(1, state.ID);
            ps.setString(2, targetPlayer.getUniqueId().toString());
            ps.setString(3, type.name());
            ps.setString(4, scope.name());
            ps.setString(5, reason);
            ps.setString(6, staffName);
            ps.setLong(7, System.currentTimeMillis());

            //Handling various cases
            if(state.type.equals(PunishmentType.KICK)){
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
                ps.setLong(8, expiresAt);
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
        }
    }

    private void applyPunishment(AddingState state){

    }

    private String createId(){
        int idLength = plugin.getConfig().getInt("punishment-id-length");
        StringBuilder id = new StringBuilder(idLength);
        SecureRandom random = new SecureRandom();

        for(int i = 0; i < idLength; i++){
            id.append(random.nextInt(10)); //Generates everytime a number from 0-9
        }

        return id.toString();
    }
}

class AddingState{
    String ID;
    OfflinePlayer targetPlayer;
    String staff;
    PunishmentType type;
    PunishmentScopes scope;
    String reason;
    long expiresAt;
    long lastInteraction;

    public AddingState(String ID, OfflinePlayer targetPlayer, String staff, PunishmentType type, PunishmentScopes scope, String reason, long expiresAt, long lastInteraction){
        this.ID = ID;
        this.targetPlayer = targetPlayer;
        this.staff = staff;
        this.type = type;
        this.scope = scope;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.lastInteraction = lastInteraction;
    }
}