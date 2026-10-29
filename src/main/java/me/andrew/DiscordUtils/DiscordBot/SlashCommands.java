//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SlashCommands extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final BotMain botMain;
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache;

    public SlashCommands(DiscordUtils plugin, BotMain botMain){
        this.plugin = plugin;
        this.botMain = botMain;

        mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        FileConfiguration botConfig = plugin.botFile().getConfig();
        event.deferReply(true).queue();

        switch (event.getName()) {
            //The 'verify' command
            case "verify" -> {
                //Checking if the user is banned
                if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                    event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                    return;
                }

                //Checking if the user is already verified
                String userId = event.getUser().getId();
                if(plugin.getPlayerVerificationCache().isUserVerifiedDiscordId(userId)) {
                    String message = botConfig.getString("already-verified-message", "You are already verified!");
                    event.getHook().sendMessage(message).queue();
                    return;
                }

                String verificationCode = event.getOption("code").getAsString();
                UUID playerUUID = plugin.getPlayerVerificationCache().getUuidFromCode(verificationCode);

                //Checking if the code is the right one
                if(playerUUID == null){
                    String message = botConfig.getString("invalid-code-message", "The code you just entered is **invalid**!");
                    event.getHook().sendMessage(message).queue();
                    return;
                }

                //Checking if the code is expired
                if(plugin.getPlayerVerificationCache().isCodeExpired(playerUUID)) {
                    String message = botConfig.getString("expired-code-message", "The code you just entered **expired**! Run **/verify** again on our Minecraft Server!");
                    event.getHook().sendMessage(message).queue();
                    return;
                }

                //Removing from the Caches
                plugin.getPlayerVerificationCache().removeFromDiscord(verificationCode);
                plugin.getPlayerVerificationCache().deleteCode(playerUUID);

                //Adding to UUID <-> DiscordId Cache
                plugin.getPlayerVerificationCache().putUuidDiscordID(playerUUID, userId);

                //Adding to DiscordId <-> UUID cache
                plugin.getPlayerVerificationCache().putDiscordIdUUID(playerUUID, userId);

                //Saving to the Database
                String sql = "INSERT INTO verifiedPlayers (uuid, discordId, verified) VALUES (?, ?, ?)";
                try(Connection conn = plugin.getDatabaseManager().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
                    ps.setString(1, playerUUID.toString());
                    ps.setString(2, userId);
                    ps.setBoolean(3, true);
                    ps.executeUpdate();
                } catch (SQLException e){
                    plugin.getLogger().warning("Couldn't insert player into playersVerification table. See message: "+e.getMessage());
                    event.getHook().sendMessage("There was a problem while doing this operation. Please contact the staff about this.").queue();
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(playerUUID);
                    assert player != null;

                    //Checking if the player is on the server
                    if(!player.isOnline()){
                        String msg = botConfig.getString("player-not-online-message", "You are not online on the server! Please join and try again.");
                        event.getHook().sendMessage(msg).queue();
                        return;
                    }

                    //Assigns the Verified role to the user and removed the Unverified one
                    Guild dcServer = plugin.getDiscordBot().getDiscordServer();
                    dcServer.retrieveMemberById(userId).queue(member -> {
                        long verifiedRoleID = plugin.botFile().getConfig().getLong("verification.verified-role-id");
                        long unverifiedRoleID = plugin.botFile().getConfig().getLong("verification.unverified-role-id");
                        Role unverifiedRole = dcServer.getRoleById(unverifiedRoleID);
                        Role verifiedRole = dcServer.getRoleById(verifiedRoleID);
                        dcServer.addRoleToMember(member, verifiedRole).queue();
                        dcServer.removeRoleFromMember(member, unverifiedRole).queue();

                        //Setting the user's nickname after their MC ign
                        member.modifyNickname(player.getName()).queue();
                    }, error -> plugin.getLogger().warning("[!] There was a problem assigning the verified role for a player. See message: "+error.getMessage()));

                    //Sends a message
                    List<String> hasVerifiedMessage = plugin.getConfig().getStringList("player-verified-message");
                    for(String line : hasVerifiedMessage){
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                    }

                    //Sound
                    Sound hasVerifiedSound =  Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-has-verified-sound", "entity.player.levelup").toLowerCase()));
                    float phvsVolume = plugin.getConfig().getInt("phvs-volume");
                    float phvsPitch = plugin.getConfig().getInt("phvs-pitch");
                    player.playSound(player.getLocation(), hasVerifiedSound, phvsVolume, phvsPitch);

                    //Giving the rewards if there are any (and if rewards are toggled)
                    boolean toggleRewards = plugin.getConfig().getBoolean("rewards.toggle-giving-rewards", false);
                    if(toggleRewards) {
                        //TO DO: Refactor Reward System.
                    }
                });

                String message = botConfig.getString("player-verified-message", "✅ You are now verified! Have fun on our server!");
                event.getHook().sendMessage(message).queue();
            }

            //pshistory command
            case "pshistory" -> {
                //Check if the user is verified
                if(!plugin.getPlayerVerificationCache().isUserVerifiedDiscordId(event.getUser().getId())) {
                    event.getHook().sendMessage("You **are not** verified! Please run */verify* on our server and try again.").queue();
                    return;
                }

                UUID userPlayerUUID = plugin.getPlayerVerificationCache().getUuidFromDiscordId(event.getUser().getId());

                if(event.getOption("ign") == null){
                    if(!plugin.getPlayerPunishmentDataCache().playerHasPunishments(userPlayerUUID)){
                        event.getHook().sendMessage("You **do not have** any punishments yet!").queue();
                        return;
                    }

                    botMain.getPunishmentHistory().displayPunishments(event, userPlayerUUID, PunishmentsFilter.ALL, true);
                    return;
                }

                //Checking if the user has permission to check the history of others
                List<Long> pshistoryRoles = botConfig.getLongList("pshistory-cmd-roles");
                if(!hasPermission(event.getMember(), pshistoryRoles)) event.getHook().sendMessage("You don't have permission to run this command!").queue();

                //Getting the target player
                String ign = event.getOption("ign").getAsString();
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);
                UUID targetPlayerUUID = targetPlayer.getUniqueId();

                //Check if the target player has any punishments
                if(!plugin.getPlayerPunishmentDataCache().playerHasPunishments(targetPlayerUUID)){
                    event.getHook().sendMessage("Player **"+ign+"** does not have any punishments yet!").queue();
                    return;
                }

                botMain.getPunishmentHistory().displayPunishments(event, targetPlayerUUID, PunishmentsFilter.ALL, false);
            }

            case "punish" -> {
                if(isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL) || isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD)){
                    event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                    return;
                }

                //Checking if the user has the necessary roles
                List<Long> punishRoles = botConfig.getLongList("punish-cmd-roles");
                if(!hasPermission(event.getMember(), punishRoles)) event.getHook().sendMessage("You don't have permission to run this command!").queue();

                //Getting the player from the ign
                String ign = event.getOption("ign").getAsString();
                UUID targetPlayerUUID = Bukkit.getPlayerUniqueId(ign);

                if (targetPlayerUUID == plugin.getPlayerVerificationCache().getUuidFromDiscordId(event.getUser().getId())) {
                    event.getHook().sendMessage("You cannot punish yourself!").queue();
                    return;
                }

                //Check if the target player has played on the server
                OfflinePlayer targetPlayer = Bukkit.getPlayer(targetPlayerUUID);
                if (!targetPlayer.hasPlayedBefore()) {
                    event.getHook().sendMessage("Player **\\" + targetPlayer.getName() + "** does not exist on the server. Please enter a valid name!").queue();
                    return;
                }

                botMain.getAddPunishments().punishPlayer(event, targetPlayer);
            }

            case "psremove" -> {
                //Checking if the user is banned
                if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                    event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                    return;
                }

                //Checking if the user has the necessary roles
                boolean hasPermission = false;
                List<Long> psRemoveRoles = botConfig.getLongList("psremove-cmd-roles");
                for(Long roleID : psRemoveRoles){
                    Role role = botMain.getDiscordServer().getRoleById(roleID);
                    if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                }
                if(!hasPermission){
                    event.getHook().sendMessage("You don't have permission to use this command!").queue();
                    return;
                }

                //Getting the ID from the command
                String ID = event.getOption("id").getAsString();
                Punishment punishment = plugin.getPlayerPunishmentDataCache().getPunishmentByID(ID);

                //Checking if there is a punishment with that ID
                if(!punishmentExists(ID)){
                    event.getHook().sendMessage("There is **no punishment** with that ID!").queue();
                    return;
                }

                Punishment p = plugin.getPlayerPunishmentDataCache().getPunishmentByID(ID);
                //Removing the punishment from DB and cache
                try {
                    plugin.getDatabaseManager().removePunishment(p.id());
                    plugin.getPlayerPunishmentDataCache().removePunishment(p.uuid(), p);
                } catch (SQLException e) {
                    plugin.getLogger().warning("[!] There was a problem removing the punishment from DB or Cache. See message: "+e.getMessage());
                    return;
                }


                //If the scope is Discord or Global, I have to unban/remove the timeout of the user
                if(punishment.scope() == PunishmentScopes.DISCORD || punishment.scope() == PunishmentScopes.GLOBAL){
                    Guild dcServer = botMain.getDiscordServer();

                    botMain.getJda().retrieveUserById(plugin.getPlayerVerificationCache().getDiscordIdFromUuid(punishment.uuid())).queue(targetUser -> {
                        if(punishment.punishmentType() == PunishmentType.PERM_BAN || punishment.punishmentType() == PunishmentType.TEMP_BAN){
                            //Removes the 'banned' role (and give him the 'verified' role) from the member if he has it
                            dcServer.retrieveMemberById(targetUser.getId()).queue(member -> {
                                long bannedRoleID = plugin.botFile().getConfig().getLong("ban-role-id");
                                Role bannedRole = dcServer.getRoleById(bannedRoleID);

                                long verifiedRoleID = plugin.botFile().getConfig().getLong("verification.verified-role-id");
                                Role verifiedRole = botMain.getDiscordServer().getRoleById(verifiedRoleID);
                                if(member.getRoles().contains(bannedRole)){
                                    dcServer.removeRoleFromMember(member, bannedRole).queue();
                                    dcServer.addRoleToMember(member, verifiedRole).queue();
                                }
                            });
                        }

                        if(punishment.punishmentType() == PunishmentType.PERM_MUTE || punishment.punishmentType() == PunishmentType.TEMP_MUTE){
                            //Removes the timeout role from the member if he has it
                            dcServer.retrieveMemberById(targetUser.getId()).queue(targetMember -> {
                                long timeoutRoleID = botConfig.getLong("timeout-role-id");
                                Role timeoutRole = dcServer.getRoleById(timeoutRoleID);
                                if(targetMember.getRoles().contains(timeoutRole)) dcServer.removeRoleFromMember(targetMember, timeoutRole).queue();
                            });

                            if(punishment.punishmentType() == PunishmentType.TEMP_MUTE) dcServer.removeTimeout(targetUser).queue();
                        }
                    });
                }

                event.getHook().sendMessage("Punishment with ID **"+ID+"** has been removed!").queue();
            }

            case "unverify" -> {
                String userID = event.getUser().getId();

                //Checking if the user is banned
                if(isUserBanned(userID, PunishmentScopes.DISCORD) || isUserBanned(userID, PunishmentScopes.GLOBAL)){
                    event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                    return;
                }

                //Checking if the user is verified
                if(!plugin.getPlayerVerificationCache().isUserVerifiedDiscordId(userID)){
                    event.getHook().sendMessage("You *don't have* any MC account linked to your DC account! Use **/verify** to link one!").queue();
                    return;
                }

                //Removing from the user the 'Verified' role and giving him the Unverified role
                long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
                Role verifiedRole = botMain.getDiscordServer().getRoleById(verifiedRoleID);

                Member targetMember = event.getMember();
                assert targetMember != null;

                if (targetMember.getRoles().contains(verifiedRole)) botMain.getDiscordServer().removeRoleFromMember(targetMember, verifiedRole).queue();

                String unverifiedRoleID = botConfig.getString("verification.unverified-role-id");
                Role unverified = botMain.getDiscordServer().getRoleById(unverifiedRoleID);
                botMain.getDiscordServer().addRoleToMember(targetMember, unverified).queue();

                //Resetting the nickname
                if (!targetMember.isOwner()) targetMember.modifyNickname(null).queue();

                //Sending the player a message in game if he is online
                Player targetPlayer = Bukkit.getPlayer(plugin.getPlayerVerificationCache().getUuidFromDiscordId(userID));
                if(targetPlayer != null && targetPlayer.isOnline()){
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String message = plugin.getConfig().getString("unverified-from-dc-message", "&4[&c&l!&4] &aYou have been unverified via the &9&lDiscord Command&a! Run &l&e/verify &ain order to verify again.");
                        message = PlaceholderAPI.setPlaceholders(targetPlayer, message);
                        targetPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));

                        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-unverified-sound", "entity.player.levelup").toLowerCase()));
                        float volume = plugin.getConfig().getInt("pus-volume", 1);
                        float pitch = plugin.getConfig().getInt("pus-pitch", 1);
                        targetPlayer.playSound(targetPlayer.getLocation(), sound, volume, pitch);
                    });
                }

                //Removing the player from the UUID <-> DiscordId caches
                plugin.getPlayerVerificationCache().removeUuidDiscordID(plugin.getPlayerVerificationCache().getUuidFromDiscordId(userID));
                plugin.getPlayerVerificationCache().removeDiscordIdUUID(userID);

                //Removing the user from the playersVerification table
                String sql = "DELETE FROM verifiedPlayers WHERE discordId = ?";

                try(Connection conn = plugin.getDatabaseManager().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, userID);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Couldn't remove user from the playersVerification table. See message: "+e.getMessage());
                }

                String message = plugin.botFile().getConfig().getString("unverified-successfully-message", "You have been unverified! If you want to verify again, run **/verify** on our Minecraft Server.");
                event.getHook().sendMessage(message).queue();
            }

            case "appeal" -> {
                String punishmentID = event.getOption("id").getAsString();

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
            }

            case "appealstatus" -> {
                //Getting the punishment ID
                String pID = event.getOption("id").getAsString();

                //If the status is 'PENDING'
                if(isPunishmentAppealInState(pID, AppealState.PENDING)) event.getHook().sendMessage("Punishment ID: "+pID+"\n**Status**: PENDING").queue();

                //If the status is 'DECLINED'
                if(isPunishmentAppealInState(pID, AppealState.DECLINED)) event.getHook().sendMessage("Punishment ID: "+pID+"\n**Status**: DECLINED").queue();

                //If the status is 'ACCEPTED'
                if(isPunishmentAppealInState(pID, AppealState.ACCEPTED)) event.getHook().sendMessage("Punishment ID: "+pID+"\n**Status**: ACCEPTED").queue();
            }

            case "startverification" -> {
                //Checking if the user is banned
                if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                    event.reply("You cannot do this because **you are banned**!").setEphemeral(true).queue();
                    return;
                }

                //Checking if the user has permission
                List<Long> startVerRoles = botConfig.getLongList("RefreshVerification-cmd-roles");
                if(!hasPermission(event.getMember(), startVerRoles)) {
                    event.getHook().sendMessage("You don't have permission to run this command!").queue();
                    return;
                }

                event.getGuild().loadMembers().onSuccess(members -> {
                    for(Member member : members){
                        //Skipping the bot and the owner.
                        if(member.getUser().isBot()) continue;
                        if(member.isOwner()) continue;

                        //Removing the roles from the list (if the member has)
                        List<Long> roleIdList = botConfig.getLongList("roles-to-be-deleted");
                        for(Long roleID : roleIdList){
                            Role roleFromList = event.getGuild().getRoleById(roleID);
                            if(member.getRoles().contains(roleFromList)) event.getGuild().removeRoleFromMember(member, roleFromList).queue();
                        }

                        //Giving the unverified role
                        long unverifiedRoleID = botConfig.getLong("verification.unverified-role-id");
                        Role unverifiedRole = event.getGuild().getRoleById(unverifiedRoleID);
                        event.getGuild().addRoleToMember(member, unverifiedRole).queue();
                    }
                });

                event.getHook().sendMessage("Verification Process started!").queue();
            }

            case "reload" -> {
                //Checking if the user is banned
                if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                    event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                    return;
                }

                //Checking if the user has permission
                List<Long> reloadRoles = botConfig.getLongList("reload-cmd-roles");
                if(!hasPermission(event.getMember(), reloadRoles)){
                    event.getHook().sendMessage("You don't have permission to run this command!").queue();
                    return;
                }

                event.getHook().sendMessage("Bot is restarting...").queue();

                //Reloading the config file
                plugin.botFile().reloadConfig();

                //Restarting the bot
                botMain.getJda().shutdownNow();

                try {
                    String token = botConfig.getString("bot-token");
                    String serverID = botConfig.getString("guild-id");
                    new BotMain(token, serverID, plugin);
                } catch (Exception e) {
                    plugin.getLogger().warning("Couldn't reload the bot! See message: "+e.getMessage());
                }
            }
        }
    }

    private boolean punishmentExists(String ID) {
        return mainCache.values().stream().flatMap(CopyOnWriteArrayList::stream).anyMatch(p -> p.id().equals(ID));
    }

    private boolean isPunishmentAppealInState(String punishmentID, AppealState state) {
        Punishment punishment = plugin.getPlayerPunishmentDataCache().getPunishmentByID(punishmentID);
        return punishment.appeal_state().equals(state);
    }

    private boolean hasPermission(Member member, List<Long> roles){
        boolean hasPermission = false;
        for(Long roleID : roles){
            Role role = botMain.getDiscordServer().getRoleById(roleID);
            if(member.getRoles().contains(role)) {hasPermission = true; break;}
        }

        return hasPermission;
    }

    private boolean isUserBanned(String userID, PunishmentScopes scope){
        return plugin.getPlayerPunishmentDataCache().isUserBanned(userID, scope);
    }
}
