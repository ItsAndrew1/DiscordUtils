//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Caching.Punishment;
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
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.*;
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
            int inactivityMinutes = plugin.botFile().getConfig().getInt("interaction-minutes");
            addingStateMap.entrySet().removeIf(entry -> now - entry.getValue().lastInteraction > TimeUnit.MINUTES.toMillis(inactivityMinutes));
        }, 0L, 20L*60); //Task runs every minute
    }

    public void punishPlayer(SlashCommandInteractionEvent event, OfflinePlayer targetPlayer) {
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

    private String getStaffName(String userId) {
        UUID staffUUID = plugin.getPlayerVerificationCache().getUuidFromDiscordId(userId);
        return Bukkit.getOfflinePlayer(staffUUID).getName();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(!event.getComponentId().equals("punishment:type") && !event.getComponentId().equals("punishment:scope")) return;

        long userId = event.getUser().getIdLong();
        AddingState state = addingStateMap.get(userId);

        if(state == null){
            event.reply("This punishment session **has expired**. Please run */punish <ign>* again!").setEphemeral(true).queue();
            return;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);
        FileConfiguration botConfig = plugin.botFile().getConfig();

        if (event.getComponentId().equals("punishment:type")) {
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
            //Setting the punishment scope
            state.scope = PunishmentScopes.valueOf(event.getValues().getFirst());

            //Checking if the target user/player is verified or not. (If the scope is DISCORD or GLOBAL)
            if((state.scope == PunishmentScopes.DISCORD || state.scope == PunishmentScopes.GLOBAL) && !plugin.getPlayerVerificationCache().isPlayerVerifiedUUID(state.targetUUID)){
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
            if(plugin.getPlayerPunishmentDataCache().isPlayerBanned(state.targetUUID, state.scope)){
                event.reply("Player **"+targetPlayer.getName()+"** is already banned in scope *"+state.scope.name()+"*!").setEphemeral(true).queue();
                addingStateMap.remove(event.getUser().getIdLong());
                return;
            }

            //Checking if the target player is already muted/timeout in the selected scope
            if(plugin.getPlayerPunishmentDataCache().isPlayerMuted(state.targetUUID, state.scope)){
                event.reply("Player **"+targetPlayer.getName()+"** is already muted in scope *"+state.scope.name()+"*!").setEphemeral(true).queue();
                addingStateMap.remove(event.getUser().getIdLong());
                return;
            }

            //Updating the last interaction
            state.lastInteraction = System.currentTimeMillis();

            //If the punishment is a temp mute or temp ban, opens the duration modal. Else, inserts the punishment.
            if ((!state.type.isPermanent() && state.type != PunishmentType.KICK) || (playerHasMaxWarnsMinus1(Bukkit.getOfflinePlayer(state.targetUUID), state.type, state.scope) && state.type != PunishmentType.PERM_MUTE_WARN && state.type != PunishmentType.PERM_BAN_WARN)){
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

                    //Applying the timeout role if the type is perm mute and scope is discord or global
                    if(state.type == PunishmentType.PERM_MUTE && (state.scope == PunishmentScopes.GLOBAL || state.scope == PunishmentScopes.DISCORD)) addTimeoutRole(state);
                    state.scope.applyPunishment(ctx, state.type);

                    //Inserting the logs (if they are toggled)
                    if(botConfig.getBoolean("use-logs", false)) new InsertLog(plugin, bot, state);

                    event.reply("Punishment **"+state.type.getName()+"** with scope **"+state.scope.name()+"** applied for player *"+targetPlayer.getName()+"*!").setEphemeral(true).queue();

                    addingStateMap.remove(event.getUser().getIdLong());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void addTimeoutRole(AddingState state) throws SQLException{
        String userID = plugin.getPlayerVerificationCache().getDiscordIdFromUuid(state.targetUUID);
        bot.getDiscordServer().retrieveMemberById(userID).queue(targetMember -> {
            long timeoutRoleID = plugin.botFile().getConfig().getLong("timeout-role-id");
            Role timeoutRole = bot.getDiscordServer().getRoleById(timeoutRoleID);

            if(timeoutRole == null) return;

            bot.getDiscordServer().addRoleToMember(targetMember, timeoutRole).queue();
        });
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event){
        if(!event.getCustomId().equals("psReason") && !event.getCustomId().equals("psDuration")) return;

        long userId =  event.getUser().getIdLong();
        AddingState state = addingStateMap.get(userId);

        //Check if the state has not expired
        if(state == null){
            event.reply("This punishment session **has expired**! Run */punish <ign>* again!").setEphemeral(true).queue();
            return;
        }

        if(event.getCustomId().equals("psReason")){
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

                //Inserting the log (if they are toggled)
                if(plugin.botFile().getConfig().getBoolean("use-logs", false)) new InsertLog(plugin, bot, state);

                //Giving the timeout role if the type is temp mute and scope is discord or global
                if(state.type == PunishmentType.TEMP_MUTE && (state.scope == PunishmentScopes.GLOBAL || state.scope == PunishmentScopes.DISCORD)) addTimeoutRole(state);

                event.reply("Punishment **"+ state.type.getName() + "** with scope **"+state.scope.name()+"** applied for player *"+targetPlayer.getName()+"*!").setEphemeral(true).queue();
                addingStateMap.remove(event.getUser().getIdLong());
            } catch (Exception e){
                event.reply("There was a problem adding the punishment. See message: "+e.getMessage()).queue();
            }
        }
    }

    private boolean playerHasMaxWarnsMinus1(OfflinePlayer targetPlayer, PunishmentType type, PunishmentScopes scope) {
        List<Punishment> playerPunishments = plugin.getPlayerPunishmentDataCache().getMainCache().get(targetPlayer.getUniqueId());
        if(playerPunishments == null) return false;

        long maxWarns = plugin.getConfig().getInt("warns-amount", 3);
        long warnNr = playerPunishments.stream().filter(p -> p.punishmentType().equals(type) && p.scope().equals(scope) && p.active()).count();

        return warnNr == maxWarns - 1;
    }

    private OfflinePlayer getPlayerStaff(String userId) {
        UUID staffUUID = plugin.getPlayerVerificationCache().getUuidFromDiscordId(userId);
        if(staffUUID == null) return null;

        return Bukkit.getOfflinePlayer(staffUUID);
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