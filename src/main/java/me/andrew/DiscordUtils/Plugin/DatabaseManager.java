//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.logging.Level;

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
             CREATE TABLE IF NOT EXISTS playersVerification(
                uuid TEXT UNIQUE PRIMARY KEY,
                ign TEXT UNIQUE,
                discordId TEXT UNIQUE,
                verified TINYINT(1) DEFAULT 0
             );
             """;
        try(PreparedStatement statement = getConnection().prepareStatement(playersTable)){
            statement.executeUpdate(playersTable);
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
        try(Statement statement = getConnection().createStatement()){
            statement.executeUpdate(punishmentsTable);
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

    public boolean isVerified(UUID uuid) throws SQLException {
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT verified FROM playersVerification WHERE uuid = ?")){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() && rs.getBoolean("verified");
            }
        }
    }



    //Helper methods for punishments
    public void setupPunishmentCache(UUID uuid){
        String sql = "SELECT * FROM punishments WHERE uuid = ? AND active = false";
        try(PreparedStatement ps = getConnection().prepareStatement(sql)){
            ps.setString(1, uuid.toString());

            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
                    PunishmentScopes scope = PunishmentScopes.valueOf(rs.getString("scope"));
                    plugin.getPlayerPunishmentDataCache().get(uuid).insertPunishment(type, scope);
                }
            }
        } catch (SQLException e){
            plugin.getLogger().warning("Couldn't setup the punishment cache! See message: "+e.getMessage());
        }
    }

    public boolean playerHasPunishments(UUID uuid) throws SQLException {
        String sql = "SELECT 1 FROM punishments WHERE uuid = ?";

        try(PreparedStatement ps = getConnection().prepareStatement(sql)){
            ps.setString(1, uuid.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    public int getPlayerActivePunishmentsNr(UUID uuid) throws SQLException {
        int activePunishments = 0;
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT active FROM punishments WHERE uuid = ?")){
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
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT active FROM punishments WHERE uuid = ?")){
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
        try(PreparedStatement ps = getConnection().prepareStatement(sql)){
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
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM punishments WHERE uuid = ? AND type = ? AND active = 1 ORDER BY created_at DESC LIMIT 1")){
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return mapPunishment(rs);
            }
        }
        return null;
    }

    public void expirePunishmentById(int crt) throws SQLException{
        try(PreparedStatement ps = getConnection().prepareStatement("UPDATE punishments SET active = false WHERE crt = ?")){
            ps.setInt(1, crt);
            ps.executeUpdate();
        }
    }

    public boolean playerHasPunishment(UUID uuid, PunishmentType type, PunishmentScopes scope) throws SQLException {
        boolean has = false;

        String statement = "SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?";
        try(PreparedStatement ps = getConnection().prepareStatement(statement)){
            ps.setString(1, uuid.toString());
            ps.setString(2, type.toString());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) has = true;
            }
        }

        return has;
    }

    public boolean isPlayerBanned(UUID uuid, PunishmentScopes scope) throws SQLException {
        boolean permBanned = false;
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
            ps.setString(1, uuid.toString());
            ps.setString(2, PunishmentType.PERM_BAN.toString());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) permBanned = true;
            }
        }

        boolean tempBanned = false;
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
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
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
            ps.setString(1, uuid.toString());
            ps.setString(2, PunishmentType.TEMP_MUTE.name());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) tempMuted = true;
            }
        }

        boolean permMuted = false;
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT 1 FROM punishments WHERE uuid = ? AND active = 1 AND type = ? AND scope = ?")){
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

        try(PreparedStatement ps = getConnection().prepareStatement("SELECT COUNT(*) AS warn_count FROM punishments WHERE uuid = ? AND type = ? AND scope = ? AND active = 1")){
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
        try(PreparedStatement ps = getConnection().prepareStatement("SELECT COUNT(*) AS warn_count FROM punishments WHERE uuid = ? AND type = ? AND scope = ? AND active = 1")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ps.setString(2, warnType.toString());
            ps.setString(3, warnScope.toString());
            try(ResultSet rs = ps.executeQuery()){
                return rs.getInt("warn_count");
            }
        }
    }

    public void expireAllWarns(OfflinePlayer targetPlayer, PunishmentType warnType, PunishmentScopes warnScope) throws SQLException {
        try(PreparedStatement ps = getConnection().prepareStatement("UPDATE punishments SET active = 0 WHERE uuid = ? AND type =? AND scope = ?")){
            ps.setString(1, targetPlayer.getUniqueId().toString());
            ps.setString(2, warnType.toString());
            ps.setString(3, warnScope.toString());
            ps.executeUpdate();
        }
    }
}
