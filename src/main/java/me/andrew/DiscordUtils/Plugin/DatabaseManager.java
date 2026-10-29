//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.DiscordBot.AppealState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//This class handles the database. It also has all the helper methods that I need
public class DatabaseManager {
    private final DiscordUtils plugin;
    private final HikariDataSource dataSource;

    public DatabaseManager(DiscordUtils plugin) {
        this.plugin = plugin;
        HikariConfig config = new HikariConfig();

        //Creates and connects the DB (Using HikariCP)
        String databaseType = plugin.getConfig().getString("database-system.type", "sqlite");

        if(databaseType.equalsIgnoreCase("sqlite")){
            //Setup for SQLite
            String fileName = plugin.getConfig().getString("database-system.file-name", "database.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);

            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setMaximumPoolSize(1);
        }
        else if(databaseType.equalsIgnoreCase("mysql")){
            //Getting the MySQL Database details
            String host = plugin.getConfig().getString("database-system.host");
            String port = plugin.getConfig().getString("database-system.port");
            String database = plugin.getConfig().getString("database-system.database");
            String username = plugin.getConfig().getString("database-system.username");
            String password = plugin.getConfig().getString("database-system.password");

            //Setup for MySQL
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername(username);
            config.setPassword(password);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }
        else{
            plugin.getLogger().severe("Invalid database type. Disabling DiscordUtils...");
            plugin.getPluginLoader().disablePlugin(plugin);
        }

        config.setPoolName("DiscordUtils Pool");
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000); //30 Minutes Max Lifetime for a connection

        this.dataSource = new HikariDataSource(config);

        //Creates the playersVerification table
        String playersTable = """
             CREATE TABLE IF NOT EXISTS verifiedPlayers(
                uuid TEXT UNIQUE PRIMARY KEY,
                discordId TEXT UNIQUE,
                verified TINYINT(1) DEFAULT 0
             );
             """;
        try(Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(playersTable)){
            statement.executeUpdate();
        } catch (SQLException e){
            plugin.getLogger().severe("Couldn't create playersVerification table. See message: "+e.getMessage());
            plugin.getLogger().severe("Disabling DiscordUtils...");
            plugin.getPluginLoader().disablePlugin(plugin);
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
        try(Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(punishmentsTable)){
            statement.executeUpdate();
        } catch (SQLException e){
            plugin.getLogger().severe("Couldn't create punishments table. See message: "+e.getMessage());
            plugin.getLogger().severe("Disabling DiscordUtils...");
            plugin.getPluginLoader().disablePlugin(plugin);
        }
    }

    public Connection getConnection() throws SQLException{
        return dataSource.getConnection();
    }
    public void closeDataSource(){
        dataSource.close();
    }

    public ConcurrentHashMap<UUID, String> getVerifiedData() {
        ConcurrentHashMap<UUID, String> verifiedData = new ConcurrentHashMap<>();
        try(Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM verifiedPlayers WHERE verified = 1")){
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    String ID = rs.getString("discordId");
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    verifiedData.put(uuid, ID);
                }
            }
        } catch (SQLException e){
            plugin.getLogger().severe("Couldn't save verification data to the memory. See message: "+e.getMessage());
            plugin.getLogger().severe("Disabling DiscordUtils...");
            plugin.getPluginLoader().disablePlugin(plugin);
        }

        return verifiedData;
    }

    public Punishment mapPunishment(ResultSet rs) throws SQLException {
        return new Punishment(
                rs.getString("id"),
                UUID.fromString(rs.getString("uuid")),
                PunishmentType.valueOf(rs.getString("type").toUpperCase()),
                PunishmentScopes.valueOf(rs.getString("scope").toUpperCase()),
                rs.getString("reason"),
                rs.getString("staff"),
                rs.getLong("created_at"),
                rs.getLong("expire_at"),
                rs.getBoolean("active"),
                rs.getBoolean("removed"),
                rs.getLong("removed_at"),
                AppealState.valueOf(rs.getString("appeal_state").toUpperCase())
        );
    }

    public void expirePunishmentById(String ID) throws SQLException{
        try(Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE punishments SET active = false WHERE id = ?")){
            ps.setString(1, ID);
            ps.executeUpdate();
        }
    }

    public void removePunishment(String ID) throws SQLException {
        long removed_at = System.currentTimeMillis();

        try(Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE punishments SET active = false, removed = true, removed_at = ? WHERE id = ? AND active = true")){
            ps.setLong(1, removed_at);
            ps.setString(2, ID);
            ps.executeUpdate();
        }
    }

    public void expireAllWarns(OfflinePlayer targetPlayer, PunishmentType warnType, PunishmentScopes warnScope) throws SQLException {
        try(Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE punishments SET active = 0 WHERE uuid = ? AND type =? AND scope = ?")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ps.setString(2, warnType.toString());
            ps.setString(3, warnScope.toString());
            ps.executeUpdate();
        }
    }
}
