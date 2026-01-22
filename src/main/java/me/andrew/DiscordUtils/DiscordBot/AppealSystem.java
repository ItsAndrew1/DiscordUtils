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
import java.util.concurrent.TimeUnit;

public class AppealSystem extends ListenerAdapter {
    private final DiscordUtils plugin;

    public AppealSystem(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event){
        FileConfiguration botConfig = plugin.botFile().getConfig();
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        if(event.getComponentId().contains("appeal:")){
            try{
                //Getting the punishmentId from the customId of the button
                String punishmentID = event.getComponentId().split(":", 2)[1];

                //Checking if the punishment still exists in the DB
                if(!doesPunishmentExist(punishmentID)){
                    event.reply("This punishment doesn't exit anymore!").setEphemeral(true).queue();
                    return;
                }

                //Checking if the appeal is in a pending state
                if(isPunishmentInAppealState(punishmentID)){
                    event.reply("You **have already appealed** for this punishment! Please *wait* until a decision is made. Only then, you may appeal again.").setEphemeral(true).queue();
                    return;
                }

                //Updating the punishment 'appeal_state' in the punishments table
                String sql = "UPDATE punishments SET appeal_state = 'PENDING' WHERE id = ?";
                try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                    ps.setString(1, punishmentID);
                    ps.executeUpdate();
                }

                //Opening a modal with the appealing form
                int maximumLength = botConfig.getInt("maximum-length-appeal");
                int minimumLength = botConfig.getInt("minimum-length-appeal");
                String placeholder = botConfig.getString("placeholder-appeal");

                TextInput reasonForm = TextInput.create("reasonForm", TextInputStyle.PARAGRAPH)
                        .setPlaceholder(placeholder)
                        .setRequired(true)
                        .setMinLength(minimumLength)
                        .setMaxLength(maximumLength).build();

                Modal formModal = Modal.create("appeal_form:"+punishmentID, "Appeal Your Punishment")
                        .addComponents(Label.of("Form", reasonForm))
                        .build();
                event.replyModal(formModal).queue();
            } catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    private boolean doesPunishmentExist(String punishmentID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT 1 FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, punishmentID);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    private boolean isPunishmentInAppealState(String punishmentID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT appeal_state FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, punishmentID);
            try(ResultSet rs = ps.executeQuery()){
                return rs.getString("appeal_state") != null;
            }
        }
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
        String sql = "SELECT uuid, type, scope";
        try {
            if(isTemporary(punishmentID)) sql+=", expiresAt";
            sql+=" FROM punishments WHERE id = ?";

            try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                ps.setString(1, punishmentID);
                ResultSet rs = ps.executeQuery();
                targetPlayerName = Bukkit.getOfflinePlayer(rs.getString("uuid")).getName();
                type = PunishmentType.valueOf(rs.getString("type"));
                scope = PunishmentScopes.valueOf(rs.getString("scope"));
                expiresAt = rs.getLong("expire_at");
                createdAt = rs.getLong("created_at");
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
                Button.success("appeal_accept:"+punishmentID, "Accept"),
                Button.danger("appeal_decline:"+punishmentID, "Decline")
        )).queue();

        event.reply("Punishment appeal sent successfully!").setEphemeral(true).queue();
    }

    private boolean isTemporary(String ID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT type FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
                return type == PunishmentType.TEMP_BAN || type == PunishmentType.TEMP_MUTE;
            }
        }
    }

    public String formatTime(long millis){
        long days =  TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        long hours =  TimeUnit.MILLISECONDS.toHours(millis);
        millis -=  TimeUnit.HOURS.toMillis(hours);

        long minutes =  TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds =  TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if(minutes > 0) seconds = 0;

        if(days > 0) sb.append(days).append("d ");
        if(hours > 0) sb.append(hours).append("h ");
        if(minutes > 0) sb.append(minutes).append("m ");
        if(seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}

