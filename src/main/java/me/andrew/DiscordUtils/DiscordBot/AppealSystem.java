//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppealSystem extends ListenerAdapter {
    private final DiscordUtils plugin;
    private final Set<String> resolvedAppeals = ConcurrentHashMap.newKeySet();

    public AppealSystem(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event){
        if(!event.getModalId().contains("appeal_form")) return;
        event.deferReply(true).queue();

        String punishmentID = event.getModalId().replace("appeal_form:", "");

        FileConfiguration botConfig = plugin.botFile().getConfig();
        String reasonForm = event.getValue("reasonForm").getAsString();
        String userId = event.getUser().getId();

        //Sends an embed with the data of the punishment tied to the ID
        long appealsChannelId = botConfig.getLong("appeal-channel-id");
        TextChannel appealsChannel = plugin.getDiscordBot().getJda().getTextChannelById(appealsChannelId);
        if(appealsChannel == null){
            plugin.getLogger().warning("[!] The Appeals Channel is null. Make sure you entered the right ID in 'botconfig.yml'!");
            event.getHook().sendMessage("There was a problem sending the Appeal Form. Try again or report this problem to the staff!").queue();
            return;
        }

        EmbedBuilder embedFormBuilder = new EmbedBuilder();
        embedFormBuilder.setTitle("Appeal for Punishment "+punishmentID);
        embedFormBuilder.setColor(Color.PINK.getRGB());
        embedFormBuilder.setFooter("User ID: "+userId);

        //Getting the punishment data from the ID
        Punishment punishment = plugin.getPlayerPunishmentDataCache().getPunishmentByID(punishmentID);

        String field =
                "Minecraft IGN: "+Bukkit.getOfflinePlayer(punishment.uuid()).getName()+"\n\n"+
                        "**Issued At**: "+formatTime(punishment.createdAt())
                        +"\n**Scope**: "+punishment.scope().name()
                        +"\n**Type**: "+punishment.punishmentType().getName();
        if(!punishment.punishmentType().isPermanent()) field+="\n**Expires In**: "+plugin.formatTime(punishment.expiresAt() - System.currentTimeMillis());
        field += "\n\n**Appeal Form**: \n" + reasonForm;
        embedFormBuilder.setDescription(field);

        //Sends the embed to the channel designated for appeals
        appealsChannel.sendMessageEmbeds(embedFormBuilder.build()).addComponents(ActionRow.of(
                Button.success("appeal_accept:"+punishmentID, "Accept"),
                Button.danger("appeal_decline:"+punishmentID, "Decline")
        )).queue();

        //Updating the status of the appeal_state of the punishment
        String sql = "UPDATE punishments SET appeal_state = ? WHERE id = ?";
        try(Connection conn = plugin.getDatabaseManager().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, "PENDING");
            ps.setString(2, punishmentID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        plugin.getPlayerPunishmentDataCache().setPunishmentAppealState(punishment, AppealState.PENDING);

        event.getHook().sendMessage("Punishment appeal sent successfully!").queue();
    }

    private String formatTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        return time.format(formatter);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event){
        if(!event.getComponentId().contains("appeal_") && !event.getComponentId().contains("getid")) return;
        event.deferReply().queue();

        FileConfiguration botConfig = plugin.botFile().getConfig();

        if(event.getComponentId().equalsIgnoreCase("getid")){
            String userID = event.getUser().getId();
            List<String> punishmentIds = getUserActivePunishmentsIDs(plugin.getPlayerVerificationCache().getUuidFromDiscordId(userID));
            if(punishmentIds == null){
                event.getHook().sendMessage("You don't have any active punishments").setEphemeral(true).queue();
                return;
            }

            //Sending the IDs
            StringBuilder ids = new StringBuilder();
            ids.append("Your Punishment ID(s) is/are: ");
            for(String punishmentID : punishmentIds){
                ids.append("**").append(punishmentID).append("**").append(" ");
            }

            event.getHook().sendMessage(ids.toString().trim()+". Run /appeal <id> to appeal your ban!").setEphemeral(true).queue();
            return;
        }

        //Getting the punishment ID
        String punishmentID =  event.getComponentId().split(":", 2)[1];

        Punishment p = plugin.getPlayerPunishmentDataCache().getPunishmentByID(punishmentID);
        if(p == null){
            resolvedAppeals.remove(punishmentID);
            event.getHook().sendMessage("There is **no punishment** with that ID!").setEphemeral(true).queue();
            return;
        }

        boolean accepted = event.getComponentId().contains("appeal_accept");

        //If the staff clicks on the Accept Button
        if(accepted){
            UUID targetPlayerUUID = p.uuid();

            String targetUserID = plugin.getPlayerVerificationCache().getDiscordIdFromUuid(targetPlayerUUID);
            if(targetUserID != null) {
                event.getGuild().retrieveMemberById(targetUserID).queue(member -> {
                    if(!member.isOwner()) {
                        //Removing the role(s) from the targetUser
                        long timeoutRoleID = botConfig.getLong("timeout-role-id", 0);
                        Role timeoutRole = event.getGuild().getRoleById(timeoutRoleID);
                        long bannedRoleID = botConfig.getLong("ban-role-id");
                        Role bannedRole = event.getGuild().getRoleById(bannedRoleID);

                        if(bannedRole != null && member.getRoles().contains(bannedRole)) event.getGuild().removeRoleFromMember(member, bannedRole).queue();
                        if(timeoutRole != null && member.getRoles().contains(timeoutRole)) event.getGuild().removeRoleFromMember(member, timeoutRole).queue();

                        //Now, if the user is verified, give him the 'Verified' role back
                        if(plugin.getPlayerVerificationCache().isUserVerifiedDiscordId(member.getId())){
                            long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
                            Role verifiedRole = event.getGuild().getRoleById(verifiedRoleID);
                            if(verifiedRole != null) event.getGuild().addRoleToMember(member, verifiedRole).queue();
                        }
                    }
                });
            }

            //Setting the appeal state as 'ACCEPTED' in db and cache
            String sql = "UPDATE punishments SET active = 0, removed = 1, removed_at = ?, appeal_state = ? WHERE id = ?";
            try(Connection conn = plugin.getDatabaseManager().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, "ACCEPTED");
                ps.setString(3, punishmentID);
                ps.executeUpdate();
            } catch (SQLException e) {
                resolvedAppeals.remove(punishmentID);
                plugin.getLogger().warning("[!] Failed to update the appeal state. See message: "+e.getMessage());
                event.getHook().sendMessage("[!] Failed to update the appeal state. See message: "+e.getMessage()).setEphemeral(true).queue();
                return;
            }

            plugin.getPlayerPunishmentDataCache().setPunishmentAppealState(p, AppealState.ACCEPTED);
            Punishment acceptedPunishment = plugin.getPlayerPunishmentDataCache().getPunishmentByID(punishmentID);
            plugin.getPlayerPunishmentDataCache().removePunishment(targetPlayerUUID, acceptedPunishment != null ? acceptedPunishment : p);

            disableAppealButtons(event, punishmentID);
            notifyPlayer(event, targetPlayerUUID, punishmentID, true);
            event.getHook().sendMessage("Appeal for **Punishment "+punishmentID+"** has been **ACCEPTED**\n**Staff**: "+event.getUser().getName()).setEphemeral(false).queue();
        }

        //If the staff clicks on the Decline Button
        if(event.getComponentId().contains("appeal_decline")){
            String sql = "UPDATE punishments SET appeal_state = ? WHERE id = ?";

            //Setting the appeal state as 'DECLINED'
            try(Connection conn = plugin.getDatabaseManager().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
                ps.setString(1, "DECLINED");
                ps.setString(2, punishmentID);
                ps.executeUpdate();
            } catch (SQLException e){
                resolvedAppeals.remove(punishmentID);
                plugin.getLogger().warning("[!] Failed to update the appeal state. See message: "+e.getMessage());
                event.getHook().sendMessage("[!] Failed to update the appeal state. See message: "+e.getMessage()).setEphemeral(true).queue();
                return;
            }

            plugin.getPlayerPunishmentDataCache().setPunishmentAppealState(p, AppealState.DECLINED);

            disableAppealButtons(event, punishmentID);
            notifyPlayer(event, p.uuid(), punishmentID, false);
            event.getHook().sendMessage("Appeal for **Punishment "+punishmentID+"** has been **DECLINED**\n**Staff**: "+event.getUser().getName()).setEphemeral(false).queue();
        }
    }

    private void disableAppealButtons(ButtonInteractionEvent event, String punishmentID){
        event.getMessage().editMessageComponents(ActionRow.of(
                Button.success("appeal_accept:"+punishmentID, "Accept").asDisabled(),
                Button.danger("appeal_decline:"+punishmentID, "Decline").asDisabled()
        )).queue();
    }

    private void notifyPlayer(ButtonInteractionEvent event, UUID targetPlayerUUID, String punishmentID, boolean accepted){
        String discordId = targetPlayerUUID == null ? null : plugin.getPlayerVerificationCache().getDiscordIdFromUuid(targetPlayerUUID);
        if(discordId == null) discordId = discordIdFromAppealMessage(event);
        if(discordId == null) return;

        String message = accepted
                ? "Your appeal for punishment **"+punishmentID+"** has been **accepted**. The punishment has been removed."
                : "Your appeal for punishment **"+punishmentID+"** has been **declined**.";

        plugin.getDiscordBot().getJda().retrieveUserById(discordId).queue(user -> user.openPrivateChannel().queue(
                channel -> channel.sendMessage(message).queue(),
                error -> plugin.getLogger().warning("[!] Couldn't DM the appeal result to "+user.getId()+". See message: "+error.getMessage())
        ), error -> plugin.getLogger().warning("[!] Couldn't find the user for appeal "+punishmentID+". See message: "+error.getMessage()));
    }

    private String discordIdFromAppealMessage(ButtonInteractionEvent event){
        if(event.getMessage().getEmbeds().isEmpty()) return null;

        MessageEmbed embed = event.getMessage().getEmbeds().get(0);
        if(embed.getFooter() == null || embed.getFooter().getText() == null) return null;

        String footer = embed.getFooter().getText();
        String prefix = "User ID: ";
        if(!footer.startsWith(prefix)) return null;

        String discordId = footer.substring(prefix.length()).trim();
        return discordId.isEmpty() ? null : discordId;
    }

    private List<String> getUserActivePunishmentsIDs(UUID targetUUID) {
        List<Punishment> punishments = plugin.getPlayerPunishmentDataCache().getMainCache().get(targetUUID);
        if(punishments == null) return null;

        List<String> activePunishmentIDs = new ArrayList<>();
        for(Punishment punishment : punishments.stream().filter(Punishment::active).toList()){
            activePunishmentIDs.add(punishment.id());
        }

        return activePunishmentIDs;
    }
}

