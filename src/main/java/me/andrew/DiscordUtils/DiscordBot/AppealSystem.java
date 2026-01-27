package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AppealSystem extends ListenerAdapter {
    private final DiscordUtils plugin;

    public AppealSystem(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event){
        if(!event.getModalId().contains("appeal_form")) return;

        FileConfiguration botConfig = plugin.botFile().getConfig();
        String reasonForm = event.getValue("reasonForm").getAsString();
        String punishmentID = event.getModalId().split(":", 2)[1];
        String userId = event.getUser().getId();

        //Sends an embed with the data of the punishment tied to the ID
        long appealsChannelId = botConfig.getLong("appeal-channel-id");
        TextChannel appealsChannel = plugin.getDiscordBot().getJda().getTextChannelById(appealsChannelId);

        EmbedBuilder embedFormBuilder = new EmbedBuilder();
        embedFormBuilder.setTitle("Appeal for Punishment "+punishmentID);
        embedFormBuilder.setColor(Color.PINK.getRGB());
        embedFormBuilder.setFooter("User ID: "+userId);

        //Getting the punishment data from the ID
        String targetPlayerName;
        PunishmentType type;
        PunishmentScopes scope;
        long expiresAt;
        long createdAt;

        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT uuid, type, scope, created_at";
        try {
            if(isTemporary(punishmentID)) sql+=", expire_at";
            sql+=" FROM punishments WHERE id = ?";

            try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                ps.setString(1, punishmentID);
                try(ResultSet rs = ps.executeQuery()){
                    if(!rs.next()) return;
                    targetPlayerName = Bukkit.getOfflinePlayer(rs.getString("uuid")).getName();
                    type = PunishmentType.valueOf(rs.getString("type"));
                    scope = PunishmentScopes.valueOf(rs.getString("scope"));
                    expiresAt = rs.getLong("expire_at");
                    createdAt = rs.getLong("created_at");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String field =
                "Minecraft IGN: "+targetPlayerName+"\n\n"+
                "**Issued At**: "+formatTime(createdAt)
                +"\n**Scope**: "+scope.name()
                +"\n**Type**: "+type.name();
        if(!type.isPermanent()) field+="\n**Expires In**: "+plugin.formatTime(expiresAt - System.currentTimeMillis());
        field += "\n\nAppeal Form: \n" + reasonForm;
        embedFormBuilder.setDescription(field);

        //Sends the embed to the channel designated for appeals
        appealsChannel.sendMessageEmbeds(embedFormBuilder.build()).addComponents(ActionRow.of(
                Button.success("appeal_accept:"+punishmentID, "Accept").withDisabled(appealWasAcceptedDecline(punishmentID)),
                Button.danger("appeal_decline:"+punishmentID, "Decline").withDisabled(appealWasAcceptedDecline(punishmentID))
        )).queue();

        //Updating the status of the appeal_state of the punishment
        String sql2 = "UPDATE punishments SET appeal_state = ? WHERE id = ?";
        try(PreparedStatement ps = dbConnection.prepareStatement(sql2)){
            ps.setString(1, "pending");
            ps.setString(2, punishmentID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        event.reply("Punishment appeal sent successfully!").setEphemeral(true).queue();
    }

    private boolean isTemporary(String ID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT type FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;

                PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
                return type == PunishmentType.TEMP_BAN || type == PunishmentType.TEMP_MUTE;
            }
        }
    }

    public String formatTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        return time.format(formatter);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event){
        if(!event.getComponentId().contains("appeal_") && !event.getComponentId().contains("getbantype")) return;

        //Getting the punishment ID
        String punishmentID =  event.getComponentId().split(":", 2)[1];
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        //If the staff clicks on the Accept Button
        if(event.getComponentId().contains("appeal_accept")){
            String sql = "UPDATE punishments SET active = 0, removed = 1, removed_at = ?, appeal_state = ? WHERE id = ?";

            try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, "accepted");
                ps.setString(3, punishmentID);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            event.reply("Appeal for **Punishment "+punishmentID+"** has been **ACCEPTED**\n**Staff**: "+event.getUser().getName()).queue();
        }

        //If the staff clicks on the Decline Button
        if(event.getComponentId().contains("appeal_decline")){
            String sql = "UPDATE punishments SET appeal_state = ? WHERE id = ?";

            //Setting the appeal state as 'Declined'
            try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                ps.setString(1, "declined");
                ps.setString(2, punishmentID);
                ps.executeUpdate();
            } catch (SQLException e){
                throw new RuntimeException(e);
            }

            event.reply("Appeal for **Punishment "+punishmentID+"** has been **DECLINED**\n**Staff**: "+event.getUser().getName()).queue();
        }

        if(event.getComponentId().equalsIgnoreCase("getbantype")){
            try {
                String userID = event.getUser().getId();
                String id = getUserBanID(UUID.fromString(getUserMcUUID(userID, dbConnection)), dbConnection);

                //Sending the ID
                event.reply("Your ban ID is: **"+id+"**. Run /appeal <id> to appeal your ban!").queue();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean appealWasAcceptedDecline(String ID){
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT appeal_state FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;
                String state = rs.getString("appeal_state");
                return !state.equals("pending");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUserMcUUID(String discordId, Connection dbConnection) throws SQLException {
        String sql = "SELECT uuid FROM playersVerification WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, discordId);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return null;
                return rs.getString("uuid");
            }
        }
    }

    private String getUserBanID(UUID targetUUID, Connection dbConnection) throws SQLException {
        String sql = "SELECT type FROM punishments WHERE uuid = ? AND active = 1";
        PunishmentType type = null;

        //Getting the type
        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, targetUUID.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return null;
                String typeString = rs.getString("type");
                if(typeString.contains("BAN") && !typeString.contains("WARN")) type = PunishmentType.valueOf(typeString);
            }
        }

        //Getting the ban ID
        try{
            String sql2 = "SELECT id FROM punishments WHERE type = ? AND active = 1";
            try(PreparedStatement ps = dbConnection.prepareStatement(sql2)){
                ps.setString(1, type.name());
                try(ResultSet rs = ps.executeQuery()){
                    if(rs.next()) return rs.getString("id");
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
}