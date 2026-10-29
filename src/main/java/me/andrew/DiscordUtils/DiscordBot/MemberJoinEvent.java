//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MemberJoinEvent extends ListenerAdapter {
    private final DiscordUtils plugin;

    public MemberJoinEvent(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        FileConfiguration botConfig = plugin.botFile().getConfig();

        //This event should only fire when the discord bot is toggled
        boolean toggleDcBot = botConfig.getBoolean("toggle-discord-bot", false);
        if(!toggleDcBot) return;

        String userId = event.getUser().getId();

        event.getGuild().retrieveMemberById(userId).queue(member -> {
            long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
            Role verifiedRole = event.getGuild().getRoleById(verifiedRoleID);

            long unverifiedRoleID = botConfig.getLong("verification.unverified-role-id");
            Role unverifiedRole = event.getGuild().getRoleById(unverifiedRoleID);

            long timeoutRoleID = botConfig.getLong("timeout-role-id");
            Role timeoutRole = event.getGuild().getRoleById(timeoutRoleID);

            long bannedRoleID = botConfig.getLong("ban-role-id");
            Role bannedRole = event.getGuild().getRoleById(bannedRoleID);

            if(verifiedRole == null || unverifiedRole == null || timeoutRole == null || bannedRole == null) return;

            //Checking if the user is verified or not
            if(!isUserVerified(userId)) event.getGuild().addRoleToMember(member, unverifiedRole).queue();
            else{
                event.getGuild().addRoleToMember(member, verifiedRole).queue();

                //Modifying their nickname after their MC ign
                member.modifyNickname(getUserIGN(member.getId())).queue();

                //Giving the user the 'Timeout' Role if he is still on timeout (on discord/globally)
                if(plugin.getPlayerPunishmentDataCache().isUserOnTimeout(member.getId(), PunishmentScopes.GLOBAL) || plugin.getPlayerPunishmentDataCache().isUserOnTimeout(member.getId(), PunishmentScopes.DISCORD)) event.getGuild().addRoleToMember(member, timeoutRole).queue();

                //Giving the user the 'Banned' Role if he is still banned (on discord / globally).
                if(plugin.getPlayerPunishmentDataCache().isUserBanned(member.getId(), PunishmentScopes.GLOBAL)|| plugin.getPlayerPunishmentDataCache().isUserBanned(member.getId(), PunishmentScopes.DISCORD)){
                    event.getGuild().removeRoleFromMember(member, verifiedRole).queue();
                    event.getGuild().addRoleToMember(member, bannedRole).queue();
                }
            }
        });
    }

    private boolean isUserVerified(String discordId) {
        return plugin.getPlayerVerificationCache().isUserVerifiedDiscordId(discordId);
    }

    private String getUserIGN(String discordId) {
        UUID userUUID = plugin.getPlayerVerificationCache().getUuidFromDiscordId(discordId);
        if(userUUID == null) return null;

        return Bukkit.getOfflinePlayer(userUUID).getName();
    }
}
