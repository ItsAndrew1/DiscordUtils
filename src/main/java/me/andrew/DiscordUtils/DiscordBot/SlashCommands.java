package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

import java.sql.SQLException;
import java.util.UUID;

public class SlashCommands extends ListenerAdapter{
    private final DiscordUtils plugin;

    public SlashCommands(DiscordUtils plugin){
        this.plugin = plugin;
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
        }
    }
}
