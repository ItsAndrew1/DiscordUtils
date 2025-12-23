package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SlashCommands extends ListenerAdapter{
    private final DiscordUtils plugin;

    public SlashCommands(DiscordUtils plugin){
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event){
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

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
                    event.reply("**Invalid** or **expired** verification code!").setEphemeral(true).queue();
                    return;
                }

                try {
                    plugin.getDatabaseManager().setPlayerVerified(uuid, userId);
                    plugin.getDatabaseManager().setPlayerHasVerified(uuid);
                    plugin.getDatabaseManager().deleteExpiredCode(uuid);
                    event.reply("✅ You are now verified! Run /verify again in Minecraft!").setEphemeral(true).queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
