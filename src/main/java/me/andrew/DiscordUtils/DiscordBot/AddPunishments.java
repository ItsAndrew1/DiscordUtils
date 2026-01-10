package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                .setPlaceholder("Choose punishment type")
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

        if(event.getInteraction().getId().equalsIgnoreCase("punishment:type")){
            switch(event.getInteraction().getValues().getFirst()){
                case "PERM_BAN" -> state.type = PunishmentType.PERM_BAN;
                case "TEMP_BAN" -> state.type = PunishmentType.TEMP_BAN;
                case "PERM_BAN_WARN" -> state.type = PunishmentType.PERM_BAN_WARN;
                case "TEMP_BAN_WARN" -> state.type = PunishmentType.TEMP_BAN_WARN;
                case "KICK" -> state.type = PunishmentType.KICK;
                case "PERM_MUTE" -> state.type = PunishmentType.PERM_MUTE;
                case "TEMP_MUTE" -> state.type = PunishmentType.TEMP_MUTE;
                case "PERM_MUTE_WARN" -> state.type = PunishmentType.PERM_MUTE_WARN;
                case "TEMP_MUTE_WARN" -> state.type = PunishmentType.TEMP_MUTE_WARN;
            }
        }

        if(event.getInteraction().getId().equalsIgnoreCase("punishment:scope")){
            switch(event.getInteraction().getValues().getFirst()){
                case "MINECRAFT" -> state.scope = PunishmentScopes.MINECRAFT;
                case "DISCORD" -> state.scope = PunishmentScopes.DISCORD;
                case "GLOBAL" -> state.scope = PunishmentScopes.GLOBAL;
            }
        }
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