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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlashCommands extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final BotMain botMain;

    public SlashCommands(DiscordUtils plugin, BotMain botMain){
        this.plugin = plugin;
        this.botMain = botMain;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
        FileConfiguration botConfig = plugin.botFile().getConfig();

        switch (event.getName()) {
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

                if (uuid == null) {
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

            //pshistory command
            case "pshistory" -> {
                OfflinePlayer userPlayer;
                try {
                    userPlayer = getUserPlayer(event.getUser().getId());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Check if the user is verified
                if (userPlayer == null) {
                    event.reply("You **are not** verified! Please run */verify* on our server and try again.").setEphemeral(true).queue();
                    return;
                }

                if (event.getOption("ign") == null) {
                    try {
                        //Check if the user has any punishments
                        if (!plugin.getDatabaseManager().playerHasPunishments(userPlayer.getUniqueId())) {
                            event.reply("You **do not have** any punishments yet!").setEphemeral(true).queue();
                            return;
                        }

                        event.deferReply().setEphemeral(true).queue();
                        botMain.getPunishmentHistory().displayPunishments(event, userPlayer.getUniqueId(), PunishmentsFilter.ALL, true);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                //Getting the target player
                String ign = event.getOption("ign").getAsString();
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);

                //Check if the target player is the user player
                if (targetPlayer == userPlayer) {
                    event.reply("You **cannot** do this! Use */pshistory* to view **your own** punishments.").setEphemeral(true).queue();
                    return;
                }

                //Checking if the player exists on the server.
                if (!targetPlayer.hasPlayedBefore()) {
                    event.reply("Player " + targetPlayer.getName() + " doesn't exist on this server! Please enter a valid name.").setEphemeral(true).queue();
                    return;
                }

                //Check if the target player has any punishments
                try {
                    if (!plugin.getDatabaseManager().playerHasPunishments(targetPlayer.getUniqueId())) {
                        event.reply("Player **\\" + targetPlayer.getName() + "** does not have any punishments yet!").setEphemeral(true).queue();
                        return;
                    }

                    event.deferReply().setEphemeral(true).queue(); //Getting the interaction hook
                    botMain.getPunishmentHistory().displayPunishments(event, targetPlayer.getUniqueId(), PunishmentsFilter.ALL, false);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            case "punish" -> {
                try {
                    //Getting the player from the ign
                    String ign = event.getOption("ign").getAsString();
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);

                    if (targetPlayer == getUserPlayer(event.getUser().getId())) {
                        event.reply("You cannot punish yourself!").setEphemeral(true).queue();
                        return;
                    }

                    //Check if the target player has played on the server
                    if (!targetPlayer.hasPlayedBefore()) {
                        event.reply("Player **\\" + targetPlayer.getName() + "** does not exist on the server. Please enter a valid name!").setEphemeral(true).queue();
                        return;
                    }

                    botMain.getAddPunishments().punishPlayer(event, targetPlayer);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private OfflinePlayer getUserPlayer(String userId) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        OfflinePlayer userPlayer = null;

        try(PreparedStatement ps = dbConnection.prepareStatement("SELECT ign FROM playersVerification WHERE discordId = ?")){
            ps.setString(1, userId);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) userPlayer = Bukkit.getOfflinePlayer(rs.getString("ign"));
            }
        }
        return userPlayer;
    }
}
