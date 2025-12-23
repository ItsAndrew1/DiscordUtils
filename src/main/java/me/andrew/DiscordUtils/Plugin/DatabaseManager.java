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
                discordId TEXT,
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

    public void setPlayerHasVerified(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE playersVerification SET hasVerified=true WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
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

    //This is for Minecraft
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

    public Connection getConnection(){
        return connection;
    }
}
