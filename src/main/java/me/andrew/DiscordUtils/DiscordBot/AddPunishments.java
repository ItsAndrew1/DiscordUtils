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
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;

public class AddPunishments extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final Map<Long, AddingState> addingStateMap = new HashMap<>();

    public AddPunishments(DiscordUtils plugin, OfflinePlayer targetPlayer, SlashCommandInteractionEvent event) {
        this.plugin = plugin;

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

        handleTask(event.getHook(), newState);
    }

    private void handleTask(InteractionHook hook, AddingState state) {
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

        hook.setEphemeral(true).sendMessage("Choose Punishment Type").addComponents(ActionRow.of(setTypeMenu)).queue();

        //Menu for punishment scope
        StringSelectMenu setScopeMenu = StringSelectMenu.create("punishment:scope")
                .setPlaceholder("Choose punishment scope")
                .setRequiredRange(1, 1)
                .addOption("Minecraft Scope", "MINECRAFT")
                .addOption("Discord Scope", "DISCORD")
                .addOption("Global Scope", "GLOBAL")
                .build();
        hook.setEphemeral(true).sendMessage("Choose Punishment Scope").addComponents(ActionRow.of(setScopeMenu)).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event){
        if(event.getInteraction().getId().equalsIgnoreCase("punishment:type")){
            switch(event.getInteraction().getValues().getFirst()){
                case "PERM_BAN" -> type = PunishmentType.PERM_BAN;
                case "TEMP_BAN" -> type = PunishmentType.TEMP_BAN;
                case "PERM_BAN_WARN" -> type = PunishmentType.PERM_BAN_WARN;
                case "PERM_MUTE" -> type = PunishmentType.PERM_MUTE;
            }
        }

        if(event.getInteraction().getId().equalsIgnoreCase("punishment:scope")){
            switch(event.getInteraction().getValues().getFirst()){
                case "MINECRAFT" -> scope
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