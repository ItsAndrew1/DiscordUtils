package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bukkit.OfflinePlayer;

public class AddPunishments extends ListenerAdapter{
    private final DiscordUtils plugin;
    private PunishmentType type;

    public AddPunishments(DiscordUtils plugin, OfflinePlayer targetPlayer, SlashCommandInteractionEvent event) {
        this.plugin = plugin;
        handleTask(targetPlayer, event.getHook());
    }

    private void handleTask(OfflinePlayer targetPlayer, InteractionHook hook) {
        StringSelectMenu menu = StringSelectMenu.create("punishment:type")
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

        hook.setEphemeral(true).sendMessage("Choose Punishment Type").addComponents(ActionRow.of(menu));
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
    }
}
