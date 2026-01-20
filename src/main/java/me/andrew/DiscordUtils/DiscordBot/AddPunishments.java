package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentContext;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
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
                null,
                targetPlayer.getUniqueId(),
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
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        AddingState state = addingStateMap.get(userId);
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);

        FileConfiguration botConfig = plugin.botFile().getConfig();

        if (event.getComponentId().equals("punishment:type")) {
            //Checking if the state has not expired
            if (!addingStateMap.containsKey(event.getUser().getIdLong())){
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

        if (event.getComponentId().equals("punishment:scope")) {
            //Check if the state has not expired
            if (!addingStateMap.containsKey(event.getUser().getIdLong())) {
                event.reply("This punishment session **has expired**. Plesae run */punish <ign>* again!").setEphemeral(true).queue();
                return;
            }

            //Setting the punishment scope
            state.scope = PunishmentScopes.valueOf(event.getValues().getFirst());

            try {
                //Checking if the target user/player is verified or not. (If the scope is DISCORD or GLOBAL)
                if((state.scope == PunishmentScopes.DISCORD || state.scope == PunishmentScopes.GLOBAL) && !plugin.getDatabaseManager().isVerified(state.targetUUID)){
                    event.reply("Player **"+targetPlayer.getName()+"** is *NOT* verified on the discord server! You may use the **MINECRAFT** scope instead.").setEphemeral(true).queue();
                    addingStateMap.remove(event.getUser().getIdLong());
                    return;
                }

                //Checking if the target player is on the game if the scope is GLOBAL or MINECRAFT and if the type is a KICK
                if((state.scope == PunishmentScopes.MINECRAFT ||  state.scope == PunishmentScopes.GLOBAL) && state.type == PunishmentType.KICK && !targetPlayer.isOnline()){
                    event.reply("Player **"+targetPlayer.getName()+"** is *NOT* online on the MC server.").setEphemeral(true).queue();
                    addingStateMap.remove(event.getUser().getIdLong());
                    return;
                }

                //Checking if the target player is already banned in the selected scope
                if(plugin.getDatabaseManager().isPlayerBanned(state.targetUUID, state.scope)){
                    event.reply("Player **"+targetPlayer.getName()+"** is already banned in scope *"+state.scope.name()+"*!").setEphemeral(true).queue();
                    addingStateMap.remove(event.getUser().getIdLong());
                    return;
                }

                //Checking if the target player is already muted/timeout in the selected scope
                if(plugin.getDatabaseManager().isPlayerMuted(state.targetUUID, state.scope)){
                    event.reply("Player **"+targetPlayer.getName()+"** is already muted in scope *"+state.scope.name()+"*!").setEphemeral(true).queue();
                    addingStateMap.remove(event.getUser().getIdLong());
                    return;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            //Updating the last interaction
            state.lastInteraction = System.currentTimeMillis();

            //If the punishment is a temp mute or temp ban, opens the duration modal. Else, inserts the punishment.
            if (!state.type.isPermanent()) {
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
                //Applies the punishment.
                try {
                    //Creating the new context and applying the punishment
                    PunishmentContext ctx = new PunishmentContext(
                            plugin,
                            getPlayerStaff(event.getUser().getId()),
                            state
                    );

                    state.scope.applyPunishment(ctx, state.type);
                    event.reply("Punishment **"+getPunishmentTypeString(state.type)+"** with scope **"+state.scope.name()+"** applied for player *"+targetPlayer.getName()+"*!").setEphemeral(true).queue();
                    addingStateMap.remove(event.getUser().getIdLong());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
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
            if(!addingStateMap.containsKey(event.getUser().getIdLong())){
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
            state.duration = parseDuration(durationString);

            //Applies the punishment
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);
            try{
                PunishmentContext ctx = new PunishmentContext(
                        plugin,
                        getPlayerStaff(event.getUser().getId()),
                        state
                );
                state.scope.applyPunishment(ctx, state.type);
                event.reply("Punishment **"+getPunishmentTypeString(state.type) + "** with scope **"+state.scope.name()+"** applied for player *"+targetPlayer.getName()+"*!").setEphemeral(true).queue();
                addingStateMap.remove(event.getUser().getIdLong());
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        }
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
}