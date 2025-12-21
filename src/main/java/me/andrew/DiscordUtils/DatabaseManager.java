//Developed by _ItsAndrew_
package me.andrew.DiscordUtils;

import java.io.File;
import java.sql.*;
import java.util.UUID;

//This class handles the database.
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
                discordID TEXT UNIQUE,
                verified BOOLEAN DEFAULT FALSE
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
                    duration INT,
                    active BOOL DEFAULT TRUE
                );
        """;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(punishmentsTable);
        }
    }

    public boolean isVerified(UUID uuid) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT verified FROM playersVerification WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getBoolean("verified");
            }
        }
    }

    public void setVerified(UUID uuid, boolean verified) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement(
                "UPDATE playersVerification SET verified = ? WHERE uuid = ?")){
            ps.setBoolean(1, verified);
            ps.setString(2, uuid.toString());
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

    public Connection getConnection() {
        return connection;
    }
}
