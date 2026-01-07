package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlashCommands extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final BotMain botMain;
    private final Map<Long, UUID> pendingHistoryRequests = new HashMap<>();

    public SlashCommands(DiscordUtils plugin, BotMain botMain){
        this.plugin = plugin;
        this.botMain = botMain;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event){
        FileConfiguration botConfig = plugin.botFile().getConfig();

        switch(event.getName()) {
            //The 'verify' command
            case "verify" -> {
                String userId = event.getUser().getId();
                int verificationCode = event.getOption("code").getAsInt();
                UUID uuid;
                try { //Getting the UUID
                    uuid = plugin.getDatabaseManager().getUuidFromCode(verificationCode);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if(uuid == null){
                    boolean ephemeral = botConfig.getBoolean("iecm-set-ephemeral");
                    String message = botConfig.getString("invalid-expired-code-message");
                    event.reply(message).setEphemeral(ephemeral).queue();
                    return;
                }

                try {
                    plugin.getDatabaseManager().setPlayerVerified(uuid, userId);
                    plugin.getDatabaseManager().setPlayerHasVerified(uuid);
                    plugin.getDatabaseManager().deleteExpiredCode(uuid);

                    String message = botConfig.getString("player-verified-message");
                    boolean ephemeral = botConfig.getBoolean("pvm-set-ephemeral");
                    event.reply(message).setEphemeral(ephemeral).queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            case "pshistory" -> {
                //Getting the target player
                String ign = event.getOption("ign").getAsString();
                OfflinePlayer targetPlayer;
                try{
                    targetPlayer = Bukkit.getOfflinePlayer(ign);
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
                if(!Arrays.stream(Bukkit.getOfflinePlayers()).toList().contains(targetPlayer)){
                    event.reply("Player "+targetPlayer.getName()+" doesn't exist on this server! Please enter a valid name.").setEphemeral(true).queue();
                    return;
                }

                //Check if the target player has any punishments
                try {
                    if(!plugin.getDatabaseManager().playerHasPunishments(targetPlayer.getUniqueId())){
                        event.reply("Player **\\"+targetPlayer.getName()+"** does not have any punishments yet!").queue();
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Adding the user to the requests.
                pendingHistoryRequests.put(event.getUser().getIdLong(), targetPlayer.getUniqueId());

                //Getting the scope with a dropdown menu
                StringSelectMenu filterMenu = StringSelectMenu.create("filter_select")
                        .setPlaceholder("All/Active/Expired Punishments")
                        .setRequiredRange(1, 1)
                        .addOption("All", "ALL")
                        .addOption("Active", "ACTIVE")
                        .addOption("Expired", "EXPIRED")
                        .build();
                event.reply("Choose the filter type").setEphemeral(true).addComponents(ActionRow.of(filterMenu)).queue();
            }
        }
    }


    @Override
    public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event){
        if(!event.getComponentId().equals("filter_select")) return;

        String option = event.getValues().getFirst();
        PunishmentsFilter filter = switch(option){
            case "ALL" -> PunishmentsFilter.ALL;
            case "ACTIVE" -> PunishmentsFilter.ACTIVE;
            case "EXPIRED" -> PunishmentsFilter.EXPIRED;
            default -> null;
        };

        UUID targetUUID = pendingHistoryRequests.remove(event.getUser().getIdLong());
        if(targetUUID == null){
            event.reply("❌ Context expired. Please run the command again!").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        try {
            botMain.getPunishmentHistory().displayPunishments(event, targetUUID, filter);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
