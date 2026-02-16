//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//This class handles the database. It also has all the helper methods that I need
public class DatabaseManager {
    private final DiscordUtils plugin;
    private Connection connection;

    public DatabaseManager(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    //Creates the database
    public void connectDb() throws SQLException {
        //Gets the database type (SQLite or MySQL)
        String databaseType = plugin.getConfig().getString("database-system.type");

        //If the type is 'sqlite', creates the database file in DiscordUtils folder
        if(databaseType.equals("sqlite")){
            String fileName = plugin.getConfig().getString("database-system.file-name");
            File dbFile = new File(plugin.getDataFolder(), fileName);

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }

        //If the type is 'mysql', sets up the connection with the data from config.yml
        if(databaseType.equals("mysql")){
            String host = plugin.getConfig().getString("database-system.host");
            String port = plugin.getConfig().getString("database-system.port");
            String database = plugin.getConfig().getString("database-system.database");
            String username = plugin.getConfig().getString("database-system.username");
            String password = plugin.getConfig().getString("database-system.password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            connection = DriverManager.getConnection(url, username, password);
        }

        //Creates the playersVerification table
        String playersTable = """
             CREATE TABLE IF NOT EXISTS playersVerification(
                uuid TEXT UNIQUE PRIMARY KEY,
                ign TEXT UNIQUE,
                discordId TEXT UNIQUE,
                verified TINYINT(1) DEFAULT 0
             );
             """;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(playersTable);
        }

        //Creates the verificationCodes table
        String verificationCodesTable = """
                CREATE TABLE IF NOT EXISTS verificationCodes(
                    uuid TEXT PRIMARY KEY UNIQUE,
                    code INT UNIQUE,
                    expire_at BIGINT
                );
                """;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(verificationCodesTable);
        }

        //Creates the punishments table
        String punishmentsTable = """
                CREATE TABLE IF NOT EXISTS punishments(
                    crt INTEGER PRIMARY KEY AUTOINCREMENT,
                    id TEXT UNIQUE,
                    uuid,
                    type TEXT,
                    scope TEXT,
                    reason TEXT,
                    staff TEXT,
                    created_at BIGINT,
                    expire_at BIGINT,
                    active TINYINT(1) DEFAULT 1,
                    removed TINYINT(1) DEFAULT 0,
                    removed_at BIGINT,
                    appeal_state TEXT DEFAULT NULL
                );
        """;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(punishmentsTable);
        }
    }

    //Helper methods for verification
    public boolean isVerified(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT verified FROM playersVerification WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getBoolean("verified");
            }
        }
    }

    public void setPlayerVerified(UUID uuid, String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE playersVerification SET verified=true, discordId=? WHERE uuid=?")){
            ps.setString(1, discordId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public boolean playerAlreadyExits(UUID uuid) throws SQLException {
        try(PreparedStatement ps =  connection.prepareStatement("SELECT 1 FROM playersVerification WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    public boolean isPlayerVerifying(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT code FROM verificationCodes WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    public boolean isCodeExpired(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT expire_at FROM verificationCodes WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;

                long expireTime = rs.getLong("expire_at");
                return expireTime < System.currentTimeMillis();
            }
        }
    }

    public void deleteExpiredCode(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM verificationCodes WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public UUID getUuidFromCode(int code) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM verificationCodes WHERE code = ?")){
            ps.setInt(1, code);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return null;

                return UUID.fromString(rs.getString("uuid"));
            }
        }
    }




    //Helper methods for punishments
    public boolean playerHasPunishments(UUID uuid) throws SQLException {
        String sql = "SELECT 1 FROM punishments WHERE uuid = ?";

        try(PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    public int getPlayerActivePunishmentsNr(UUID uuid) throws SQLException {
        int activePunishments = 0;
        try(PreparedStatement ps = connection.prepareStatement("SELECT active FROM punishments WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    boolean activePunishment = rs.getBoolean("active");
                    if(activePunishment) activePunishments++;
                }
                return activePunishments;
            }
        }
    }

    public int getPlayerExpiredPunishmentsNr(UUID uuid) throws SQLException {
        int inactivePunishments = 0;
        try(PreparedStatement ps = connection.prepareStatement("SELECT active FROM punishments WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    boolean activePunishment = rs.getBoolean("active");
                    if(!activePunishment) inactivePunishments++;
                }
                return inactivePunishments;
            }
        }
    }

    public List<Punishment> getPlayerPunishments(UUID uuid, PunishmentsFilter filter, int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM punishments WHERE uuid = ?";
        if(filter.equals(PunishmentsFilter.ACTIVE)){
            sql += " AND active = 1";
        }
        else if(filter.equals(PunishmentsFilter.EXPIRED)){
            sql += " AND active = 0";
        }

        sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try(PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try(ResultSet rs = ps.executeQuery()){
                List<Punishment> punishments = new ArrayList<>();
                while(rs.next()){
                    punishments.add(mapPunishment(rs));
                }
                return punishments;
            }
        }
    }

    public Punishment mapPunishment(ResultSet rs) throws SQLException {
        return new Punishment(
                PunishmentType.valueOf(rs.getString("type")),
                rs.getInt("crt"),
                rs.getString("id"),
                UUID.fromString(rs.getString("uuid")),
                PunishmentScopes.valueOf(rs.getString("scope")),
                rs.getString("reason"),
                rs.getString("staff"),
                rs.getLong("created_at"),
                rs.getLong("expire_at"),
                rs.getBoolean("active"),
                rs.getBoolean("removed"),
                rs.getLong("removed_at")
        );
    }

    public Punishment getPunishment(UUID uuid, PunishmentType type) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM punishments WHERE uuid = ? AND type = ? AND active = 1 ORDER BY created_at DESC LIMIT 1")){
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return mapPunishment(rs);
            }
        }
        return null;
    }

    public void expirePunishmentById(int crt) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active = false WHERE crt = ?")){
            ps.setInt(1, crt);
            ps.executeUpdate();
        }
    }

    public boolean isPlayerBanned(UUID uuid, PunishmentScopes scope) throws SQLException {
        boolean permBanned = false;
        try(PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
            ps.setString(1, uuid.toString());
            ps.setString(2, PunishmentType.PERM_BAN.toString());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) permBanned = true;
            }
        }

        boolean tempBanned = false;
        try(PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
            ps.setString(1, uuid.toString());
            ps.setString(2, PunishmentType.TEMP_BAN.toString());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) tempBanned = true;
            }
        }

        return  tempBanned || permBanned;
    }

    public boolean isPlayerMuted(UUID uuid, PunishmentScopes scope) throws SQLException {
        boolean tempMuted = false;
        try(PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
            ps.setString(1, uuid.toString());
            ps.setString(2, PunishmentType.TEMP_MUTE.name());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) tempMuted = true;
            }
        }

        boolean permMuted = false;
        try(PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
            ps.setString(1, uuid.toString());
            ps.setString(2, PunishmentType.PERM_MUTE.name());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) permMuted = true;
            }
        }

        return permMuted || tempMuted;
    }

    public void removePunishment(PunishmentType type, UUID targetPlayerUUID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        long removed_at = System.currentTimeMillis();

        try(PreparedStatement ps = dbConnection.prepareStatement("UPDATE punishments SET active = false, removed = true, removed_at = ? WHERE uuid = ? AND type = ? AND active = true")){
            ps.setLong(1, removed_at);
            ps.setString(2, targetPlayerUUID.toString());
            ps.setString(3, type.toString());
            ps.executeUpdate();
        }
    }

    public boolean playerHasTheNrOfWarns(OfflinePlayer targetPlayer, PunishmentType warnType, PunishmentScopes warnScope) throws SQLException {
        int warnNr = 0;
        int maxWarns = plugin.getConfig().getInt("warns-amount");

        try(PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) AS warn_count FROM punishments WHERE uuid = ? AND type = ? AND scope = ? AND active = 1")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ps.setString(2, warnType.toString());
            ps.setString(3, warnScope.toString());

            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) warnNr = rs.getInt("warn_count");
            }
        }

        return warnNr == maxWarns;
    }

    public int getNrOfWarns(OfflinePlayer targetPlayer, PunishmentType warnType, PunishmentScopes warnScope) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) AS warn_count FROM punishments WHERE uuid = ? AND type = ? AND scope = ? AND active = 1")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ps.setString(2, warnType.toString());
            ps.setString(3, warnScope.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.getInt("warn_count");
            }
        }
    }

    public void expireAllWarns(OfflinePlayer targetPlayer, PunishmentType warnType, PunishmentScopes warnScope) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active = 0 WHERE uuid = ? AND type =? AND scope = ?")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ps.setString(2, warnType.toString());
            ps.setString(3, warnScope.toString());
            ps.executeUpdate();
        }
    }

    public Connection getConnection(){
        return connection;
    }
}
