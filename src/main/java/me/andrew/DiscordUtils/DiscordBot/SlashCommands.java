package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

                //Check if the user is already verified
                try{
                    if(plugin.getDatabaseManager().isVerifiedDc(userId)){
                        event.reply("You are already **verified**!")
                                .setEphemeral(true)
                                .queue();
                        return;
                    }
                } catch (SQLException e){
                    throw new RuntimeException(e);
                }

                //Inserts the user ID into the verificationCodes and playersVerification tables
                try(PreparedStatement ps = dbConnection.prepareStatement("INSERT INTO verificationCodes (discordId) VALUES (?)")){
                    ps.setString(1, userId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                try(PreparedStatement ps = dbConnection.prepareStatement("INSERT INTO playersVerification (discordId) VALUES (?)")){
                    ps.setString(1, userId);
                    ps.executeUpdate();
                }catch(SQLException e){
                    throw new RuntimeException(e);
                }

                //Check if the code from minecraft is already expired.
                int verificationCode = event.getOption("code").getAsInt();
                try {
                    if(plugin.getDatabaseManager().isCodeExpiredDiscord(userId)){
                        plugin.getDatabaseManager().deleteExpiredCodeDc(userId);
                        plugin.getDatabaseManager().deleteDiscordId(userId);
                        event.reply("Code **"+verificationCode+"** has expired! Please run **/verify** again in minecraft and try again!")
                                .setEphemeral(true)
                                .queue();
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Check if the verification code is the right one
                try {
                    if(!plugin.getDatabaseManager().isCodeRight(userId, verificationCode)){
                        plugin.getDatabaseManager().deleteDiscordId(userId);
                        event.reply("Code **"+verificationCode+"** is not the right one! Please _try again_!").queue();
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Sets the player as verified
                try {
                    plugin.getDatabaseManager().setPlayerVerified(userId);
                    String username = event.getUser().getName();
                    event.reply("Congrats **"+username+"**! You have been verified. Please run _/verify_ again in Minecraft!").queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
