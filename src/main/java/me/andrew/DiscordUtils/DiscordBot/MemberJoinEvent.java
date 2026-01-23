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

public class MemberJoinEvent extends ListenerAdapter {
    private final DiscordUtils plugin;
    private final BotMain bot;

    public MemberJoinEvent(DiscordUtils plugin, BotMain bot) {
        this.plugin = plugin;
        this.bot = bot;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild dcServer = bot.getDiscordServer();
        if(event.getGuild() != dcServer) return;

        FileConfiguration botConfig = plugin.botFile().getConfig();

        try {
            //Checking if the user is verified => gives Verified role
            String verifiedRoleID = botConfig.getString("verification.verified-role-id");
            Role verifiedRole = dcServer.getRoleById(verifiedRoleID);
            if(isUserVerified(event.getUser().getId())){
                dcServer.addRoleToMember(event.getMember(), verifiedRole).queue();
                return;
            }

            //Giving the unverified role
            String unverifiedRoleID = botConfig.getString("verification.unverified-role-id");
            Role unverifiedRole = dcServer.getRoleById(unverifiedRoleID);
            dcServer.addRoleToMember(event.getMember(), unverifiedRole).queue();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
}
