//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

//This class handles the database. It also has all the helper methods that I need
public class DatabaseManager {
    private final DiscordUtils plugin;
    private Connection connection;

    public DatabaseManager(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    //Creates the database
    public void createDb() throws SQLException {
        //Creates the 'database.db' inside the DiscordUtils folder
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        String connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(connectionString);

        //Creates the playersVerification table
        String playersTable = """
             CREATE TABLE IF NOT EXISTS playersVerification(
                uuid TEXT PRIMARY KEY,
                discordId TEXT UNIQUE,
                verified BOOLEAN DEFAULT FALSE,
                hasVerified BOOLEAN DEFAULT FALSE
             );
             """;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(playersTable);
        }

        //Creates the verificationCodes table
        String verificationCodesTable = """
                CREATE TABLE IF NOT EXISTS verificationCodes(
                    uuid TEXT PRIMARY KEY UNIQUE,
                    discordId TEXT,
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
                    discordID PRIMARY KEY,
                    uuid TEXT,
                    type TEXT,
                    reason TEXT,
                    staff TEXT,
                    duration BIGINT,
                    active BOOL DEFAULT TRUE
                );
        """;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(punishmentsTable);
        }
    }

    //This is for minecraft (UUID)
    public boolean isVerifiedMC(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT verified FROM playersVerification WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getBoolean("verified");
            }
        }
    }

    //This is for the bot (discord ID)
    public boolean isVerifiedDc(String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT verified FROM playersVerification WHERE discordId = ?")){
            ps.setString(1, discordId);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getBoolean("verified");
            }
        }
    }

    public void setPlayerVerified(String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE playersVerification SET verified=true WHERE discordId = ?")){
            ps.setString(1, discordId);
            ps.executeUpdate();
        }
    }

    public void setPlayerHasVerified(String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE playersVerification SET hasVerified=true WHERE discordId = ?")){
            ps.setString(1, discordId);
            ps.executeUpdate();
        }
    }

    public boolean hasPlayerVerified(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT hasVerified FROM playersVerification WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getBoolean("hasVerified");
            }
        }
    }

    public void deleteDiscordId(String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE playersVerification SET discordId=null WHERE discordId = ?")){
            ps.setString(1, discordId);
            ps.executeUpdate();
        }
    }

    public boolean playerAlreadyExits(UUID uuid) throws SQLException {
        try(PreparedStatement ps =  connection.prepareStatement(
                "SELECT 1 FROM playersVerification WHERE uuid = ?")){
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

    public boolean isCodeRight(String discordId, int code) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT code FROM verificationCodes WHERE discordId = ?")){
            ps.setString(1, discordId);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getInt("code") == code;
            }
        }
    }

    //This is for Minecraft
    public boolean isCodeExpiredMC(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT expire_at FROM verificationCodes WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;

                long expireTime = rs.getLong("expire_at");
                return expireTime < System.currentTimeMillis();
            }
        }
    }

    //This is for discord
    public boolean isCodeExpiredDiscord(String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT expire_at FROM verificationCodes WHERE discordId = ?")){
            ps.setString(1, discordId);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;

                long expireTime = rs.getLong("expire_at");
                return expireTime < System.currentTimeMillis();
            }
        }
    }

    public void deleteExpiredCodeMC(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM verificationCodes WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void deleteExpiredCodeDc(String discordId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM verificationCodes WHERE discordId = ?")){
            ps.setString(1, discordId);
            ps.executeUpdate();
        }
    }

    public Connection getConnection(){
        return connection;
    }
}
