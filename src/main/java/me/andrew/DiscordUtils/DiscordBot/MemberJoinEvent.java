package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class MemberJoinEvent extends ListenerAdapter {
    private final DiscordUtils plugin;

    public MemberJoinEvent(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        FileConfiguration botConfig = plugin.botFile().getConfig();
        String userId = event.getUser().getId();

        event.getGuild().retrieveMemberById(userId).queue(member -> {
            long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
            Role verifiedRole = event.getGuild().getRoleById(verifiedRoleID);

            long unverifiedRoleID = botConfig.getLong("verification.unverified-role-id");
            Role unverifiedRole = event.getGuild().getRoleById(unverifiedRoleID);

            if(verifiedRole == null || unverifiedRole == null) return;

            try {
                if(isUserVerified(event.getUser().getId())){
                    event.getGuild().addRoleToMember(member, verifiedRole).queue();

                    //Modifying their nickname after their MC ign
                    member.modifyNickname(getUserIGN(member.getId())).queue();
                    return;
                }

                event.getGuild().addRoleToMember(member, unverifiedRole).queue();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean isUserVerified(String discordId) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String SQL = "SELECT 1 FROM playersVerification WHERE discordId = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(SQL)){
            ps.setString(1, discordId);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    private String getUserIGN(String discordId) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String SQL = "SELECT ign FROM playersVerification WHERE discordId = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(SQL)){
            ps.setString(1, discordId);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return rs.getString("ign");
                return null;
            }
        }
    }
}
