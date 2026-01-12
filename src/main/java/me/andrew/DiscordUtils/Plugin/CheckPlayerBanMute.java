//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CheckPlayerBanMute implements Listener{
    private final DiscordUtils plugin;

    public CheckPlayerBanMute(DiscordUtils plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) throws SQLException {
        Player player = e.getPlayer();

        //Checks if the player is perm banned
        Punishment permBan = plugin.getDatabaseManager().getPunishment(player.getUniqueId(), PunishmentType.PERM_BAN);
        if(permBan != null){
            if(!permBan.isActive()){
                plugin.getDatabaseManager().expirePunishmentById(permBan.getCrt());
                e.allow();
            }
            else{
                //Displays a configurable message on the player's screen
                //Getting the reason
                String reason = permBan.getReason();

                //Displaying the message
                String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.perm-ban-message"));
                e.disallow(PlayerLoginEvent.Result.KICK_BANNED, message
                        .replace("%scope%", getColoredScope(permBan.getScope()))
                        .replace("%reason%", reason)
                        .replace("%id%", permBan.getId())
                );
            }
            return;
        }

        //Check if the player is temporarily banned
        Punishment tempBan = plugin.getDatabaseManager().getPunishment(player.getUniqueId(), PunishmentType.TEMP_BAN);
        if(tempBan != null){
            if(isPunishmentExpired(tempBan)){
                plugin.getDatabaseManager().expirePunishmentById(tempBan.getCrt());
                e.allow();
            }
            else{
                //Displays the message, same as before
                //Getting the reason
                String reason = tempBan.getReason();

                //Getting and formatting the duration
                long expiredAt = tempBan.getExpiresAt();
                Instant createdInstant = Instant.ofEpochMilli(expiredAt);
                LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"); //Make configurable!!!
                String expiresAt = time.format(formatter);

                long duration = expiredAt - System.currentTimeMillis();
                String durationString = plugin.formatTime(duration);

                //Displaying the message
                String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-punishments-messages.temp-ban-message"));
                e.disallow(PlayerLoginEvent.Result.KICK_BANNED, message
                        .replace("%scope%", getColoredScope(tempBan.getScope()))
                        .replace("%reason%", reason)
                        .replace("%time_left%", durationString)
                        .replace("%expiration_time%", expiresAt)
                        .replace("%id%", tempBan.getId())
                );
            }
            return;
        }

        e.allow();
    }
    private String getColoredScope(PunishmentScopes scope){
        return switch(scope){
            case PunishmentScopes.DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case PunishmentScopes.GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
            case PunishmentScopes.MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
        };
    }
    private boolean isPunishmentExpired(Punishment p){
        return p.getExpiresAt() <= System.currentTimeMillis();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) throws SQLException{
        Player player = e.getPlayer();

        //Check if the player is perm muted
        Punishment permMute = plugin.getDatabaseManager().getPunishment(player.getUniqueId(), PunishmentType.PERM_MUTE);
        if(permMute != null){
            if(permMute.isActive()){
                e.setCancelled(true);

                //Sends the target player a message (if the messages are toggled)

                //Getting the reason and scope
                String reason = permMute.getReason();
                String scope = getColoredScope(permMute.getScope());

                //Sending the message
                List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-mute-message");
                for(String messageLine: message) player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                        .replace("%scope%", scope)
                        .replace("%reason%", reason)
                        .replace("%id%", permMute.getId())
                ));
            }
        }

        //Check if the player is temp muted
        Punishment tempMute = plugin.getDatabaseManager().getPunishment(player.getUniqueId(), PunishmentType.TEMP_MUTE);
        if(tempMute != null){
            if(isPunishmentExpired(tempMute)) plugin.getDatabaseManager().expirePunishmentById(tempMute.getCrt());
            else{
                e.setCancelled(true);

                //Sends the player a message (if the messages are toggled)
                boolean toggleMessage = plugin.getConfig().getBoolean("player-punishments-messages.toggle");
                if(toggleMessage){
                    //Getting the reason
                    String reason = tempMute.getReason();

                    //Getting the scope
                    String scope =  getColoredScope(tempMute.getScope());

                    //Getting the durations
                    long expiresAt = tempMute.getExpiresAt();
                    Instant expiresAtInstant = Instant.ofEpochMilli(expiresAt);
                    LocalDateTime time = LocalDateTime.ofInstant(expiresAtInstant, ZoneId.systemDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"); //Make configurable!!!
                    String expiresAtString = time.format(formatter);

                    long timeLeft = expiresAt - System.currentTimeMillis();
                    String timeLeftString =  plugin.formatTime(timeLeft);

                    //Sends the message to the player
                    List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-message");
                    for(String messageLine : message) player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                            .replace("%scope%", scope)
                            .replace("%reason%", reason)
                            .replace("%time_left%", timeLeftString)
                            .replace("%expiration_time%", expiresAtString)
                            .replace("%id%", tempMute.getId())
                    ));
                }
            }
        }
    }
}
