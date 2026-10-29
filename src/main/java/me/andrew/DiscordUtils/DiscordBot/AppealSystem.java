//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppealSystem extends ListenerAdapter {
    private final DiscordUtils plugin;

    public AppealSystem(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event){
        if(!event.getModalId().contains("appeal_form")) return;
        event.deferReply(true).queue();

        String punishmentID = event.getModalId().replace("appeal_form:", "");

        //Checking if the punishment still exists and hasn't expired
        if(!punishmentIsActive(punishmentID)){
            event.getHook().sendMessage("This punishment has already been **removed** or has **expired**!").queue();
            return;
        }

        //Checking if the appeal was declined
        if(wasAppealDeclined(punishmentID)){
            event.getHook().sendMessage("This appeal has been declined. You cannot appeal again.").queue();
            return;
        }

        //Checking if the punishment is already in appeal state
        if(isAppealInPendingState(punishmentID)){
            event.getHook().sendMessage("You have already sent an appeal for this punishment! Please be patient while our staff reviews your appeal.").queue();
            return;
        }

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
        event.deferReply(true).queue();

        FileConfiguration botConfig = plugin.botFile().getConfig();

        if(event.getComponentId().equalsIgnoreCase("getid")){
            String userID = event.getUser().getId();
            List<String> punishmentIds = getUserActivePunishmentsIDs(plugin.getPlayerVerificationCache().getUuidFromDiscordId(userID));
            assert punishmentIds != null;

            //Sending the IDs
            StringBuilder ids = new StringBuilder();
            ids.append("Your Punishment ID(s) is/are: ");
            for(String punishmentID : punishmentIds){
                ids.append("**").append(punishmentID).append("**").append(" ");
            }

            event.reply("Your Punishment ID(s) is/are: "+ids.toString().trim()+". Run /appeal <id> to appeal your ban!").setEphemeral(true).queue();
            return;
        }

        //Getting the punishment ID
        String punishmentID =  event.getComponentId().split(":", 2)[1];

        //Checking if the appeal has been already solved
        if(appealWasAcceptedDecline(punishmentID)){
            event.getHook().sendMessage("This appeal has been solved.").queue();
            return;
        }

        Punishment p = plugin.getPlayerPunishmentDataCache().getPunishmentByID(punishmentID);

        //If the staff clicks on the Accept Button
        if(event.getComponentId().contains("appeal_accept")){
            UUID targetPlayerUUID = getPlayerUuidFromID(punishmentID);

            String targetUserID = plugin.getPlayerVerificationCache().getDiscordIdFromUuid(targetPlayerUUID);
            if(targetUserID != null) {
                event.getGuild().retrieveMemberById(targetUserID).queue(member -> {
                    //Removing the role(s) from the targetUser
                    long timeoutRoleID = botConfig.getLong("timeout-role-id", 0);
                    Role timeoutRole = event.getGuild().getRoleById(timeoutRoleID);
                    long bannedRoleID = botConfig.getLong("ban-role-id");
                    Role bannedRole = event.getGuild().getRoleById(bannedRoleID);

                    if(member.getRoles().contains(bannedRole)) event.getGuild().removeRoleFromMember(member, bannedRole).queue();
                    if(member.getRoles().contains(timeoutRole)) event.getGuild().removeRoleFromMember(member, timeoutRole).queue();

                    //Now, if the user is verified, give him the 'Verified' role back
                    if(plugin.getPlayerVerificationCache().isUserVerifiedDiscordId(member.getId())){
                        long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
                        Role verifiedRole = event.getGuild().getRoleById(verifiedRoleID);
                        event.getGuild().addRoleToMember(member, verifiedRole).queue();
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
                plugin.getLogger().warning("[!] Failed to update the appeal state. See message: "+e.getMessage());
                event.getHook().sendMessage("[!] Failed to update the appeal state. See message: "+e.getMessage()).queue();
                return;
            }

            plugin.getPlayerPunishmentDataCache().setPunishmentAppealState(p, AppealState.ACCEPTED);

            event.getHook().sendMessage("Appeal for **Punishment "+punishmentID+"** has been **ACCEPTED**\n**Staff**: "+event.getUser().getName()).queue();
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
                plugin.getLogger().warning("[!] Failed to update the appeal state. See message: "+e.getMessage());
                event.getHook().sendMessage("[!] Failed to update the appeal state. See message: "+e.getMessage()).queue();
                return;
            }

            plugin.getPlayerPunishmentDataCache().setPunishmentAppealState(p, AppealState.DECLINED);

            event.getHook().sendMessage("Appeal for **Punishment "+punishmentID+"** has been **DECLINED**\n**Staff**: "+event.getUser().getName()).queue();
        }
    }

    private UUID getPlayerUuidFromID(String id){
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();
        for(UUID uuid : mainCache.keySet()){
            CopyOnWriteArrayList<Punishment> punishments = mainCache.get(uuid);
            if(punishments != null && punishments.stream().anyMatch(p -> p.id().equals(id))) return uuid;
        }
        return null;
    }

    private boolean appealWasAcceptedDecline(String ID){
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();
        Punishment punishment = mainCache.values().stream().flatMap(CopyOnWriteArrayList::stream).filter(p -> p.id().equals(ID)).findFirst().orElse(null);
        if(punishment == null) return false;

        return punishment.appeal_state().equals(AppealState.ACCEPTED) || punishment.appeal_state().equals(AppealState.DECLINED);
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

    private boolean punishmentIsActive(String ID) {
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();

        return mainCache.values().stream().flatMap(CopyOnWriteArrayList::stream).anyMatch(p -> p.id().equals(ID) && p.active());
    }

    private boolean wasAppealDeclined(String ID) {
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();

        return mainCache.values().stream().flatMap(CopyOnWriteArrayList::stream).anyMatch(p -> p.id().equals(ID) && p.appeal_state().equals(AppealState.DECLINED));
    }

    private boolean isAppealInPendingState(String ID) {
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();

        return mainCache.values().stream().flatMap(CopyOnWriteArrayList::stream).anyMatch(p -> p.id().equals(ID) && p.appeal_state().equals(AppealState.PENDING));
    }
}

