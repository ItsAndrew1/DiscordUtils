//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.PreparedStatement;
import java.sql.SQLException;

//Adds each player that joins to the database (if they are not in it already)
public class PlayerJoin implements Listener {
    private final DiscordUtils plugin;

    public PlayerJoin(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();

        //Check if the player already is in the db
        if(plugin.getDatabaseManager().playerAlreadyExits(player.getUniqueId())) return;

        //Adds the player in the db
        try(PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("INSERT INTO playersVerification (uuid, discordID, verified) VALUES (?, ?, ?)")){
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, null);
            ps.setBoolean(3, false);
            ps.executeUpdate();
        }
    }
}
