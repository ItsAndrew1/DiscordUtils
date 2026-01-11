package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddPunishments extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final Map<Long, AddingState> addingStateMap = new HashMap<>();

    public AddPunishments(DiscordUtils plugin) {
        this.plugin = plugin;

        //Task for auto deleting users from the map
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            addingStateMap.entrySet().removeIf(entry -> now - entry.getValue().lastInteraction > TimeUnit.MINUTES.toMillis(2));
        }, 0L, 20L*60); //Task runs every minute
    }

    public void punishPlayer(SlashCommandInteractionEvent event, OfflinePlayer targetPlayer) {
        //Creating the state
        AddingState newState = new AddingState(
                targetPlayer,
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
                event.reply("Punishment **"+getPunishmentString(state.type)+"** for player *"+state.targetPlayer.getName()+"* applied successfully!").setEphemeral(true).queue();
                try {
                    insertPunishment(state);
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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            event.reply("Punishment **"+getPunishmentString(state.type)+"** for player *\\"+state.targetPlayer.getName()+"* applied successfully!").setEphemeral(true).queue();
        }
    }

    private boolean isTemporary(PunishmentType type){
        return type == PunishmentType.TEMP_BAN || type == PunishmentType.TEMP_MUTE;
    }

    private String getPunishmentString(PunishmentType type){
        return switch(type){
            case PunishmentType.KICK -> ChatColor.translateAlternateColorCodes('&', "KICK");
            case PunishmentType.PERM_BAN -> ChatColor.translateAlternateColorCodes('&', "PERMANENT BAN");
            case PunishmentType.PERM_BAN_WARN -> ChatColor.translateAlternateColorCodes('&', "PERMANENT BAN WARN");
            case PunishmentType.PERM_MUTE -> ChatColor.translateAlternateColorCodes('&', "PERMANENT MUTE");
            case PunishmentType.PERM_MUTE_WARN -> ChatColor.translateAlternateColorCodes('&', "PERMANENT MUTE WARN");
            case PunishmentType.TEMP_BAN -> ChatColor.translateAlternateColorCodes('&', "TEMPORARY BAN");
            case PunishmentType.TEMP_BAN_WARN -> ChatColor.translateAlternateColorCodes('&', "TEMPORARY BAN WARN");
            case PunishmentType.TEMP_MUTE -> ChatColor.translateAlternateColorCodes('&', "TEMPORARY MUTE");
            case PunishmentType.TEMP_MUTE_WARN -> ChatColor.translateAlternateColorCodes('&', "TEMPORARY MUTE WARN");
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
        PunishmentType type = state.type;
        PunishmentScopes scope = state.scope;
        long expiresAt = state.expiresAt;

        try(PreparedStatement ps = dbConnection.prepareStatement("""
                INSERT INTO punishments (id, uuid, type, scope, reason, staff, created_at, expire_at, active, removed, removed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)){

        }
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
    OfflinePlayer targetPlayer;
    PunishmentType type;
    PunishmentScopes scope;
    String reason;
    long expiresAt;
    long lastInteraction;

    public AddingState(OfflinePlayer targetPlayer, PunishmentType type, PunishmentScopes scope, String reason, long expiresAt, long lastInteraction){
        this.targetPlayer = targetPlayer;
        this.type = type;
        this.scope = scope;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.lastInteraction = lastInteraction;
    }
}