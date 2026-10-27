//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.andrew.DiscordUtils.Caching.Punishment;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        //This event should only fire when the discord bot is toggled
        boolean toggleDcBot = plugin.botFile().getConfig().getBoolean("toggle-discord-bot", false);
        if(!toggleDcBot) return;

        Player player = e.getPlayer();

        //Checks if the player is perm banned (with scope minecraft/global)
        Punishment permBan = plugin.getPlayerPunishmentDataCache().getPunishment(player.getUniqueId(), PunishmentType.PERM_BAN);
        if(permBan != null && (permBan.scope() == PunishmentScopes.MINECRAFT || permBan.scope() == PunishmentScopes.GLOBAL)){
            if(!permBan.active()){
                plugin.getDatabaseManager().expirePunishmentById(permBan.id());
                plugin.getPlayerPunishmentDataCache().expirePunishmentById(player.getUniqueId(), permBan.id());
                e.allow();
            }
            else{
                //Displays a configurable message on the player's screen
                //Getting the reason
                String reason = permBan.reason();

                //Displaying the message
                String message =  plugin.getConfig().getString("player-punishments-messages.perm-ban-message", "&cYou are permanently banned from this server.\\n\\n&7Scope: %scope%\\n&7Reason: &f%reason%\\n\\n&7Ban ID: &f%id%\\n&7Use this ID to appeal.");
                message = plugin.parsePP(player, message);
                message = message.replace("%scope%", getColoredScope(permBan.scope())).replace("%reason%", reason).replace("%id%", permBan.id());
                e.disallow(PlayerLoginEvent.Result.KICK_BANNED, LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            }
            return;
        }

        //Check if the player is temporarily banned (with scope minecraft/global)
        Punishment tempBan = plugin.getPlayerPunishmentDataCache().getPunishment(player.getUniqueId(), PunishmentType.PERM_BAN);
        if(tempBan != null && (tempBan.scope() == PunishmentScopes.MINECRAFT || tempBan.scope() == PunishmentScopes.GLOBAL)){
            if(isPunishmentExpired(tempBan)){
                plugin.getDatabaseManager().expirePunishmentById(tempBan.id());
                plugin.getPlayerPunishmentDataCache().expirePunishmentById(player.getUniqueId(), tempBan.id());
                e.allow();
            }
            else{
                //Displays the message, same as before
                //Getting the reason
                String reason = tempBan.reason();

                //Getting and formatting the duration
                long expiredAt = tempBan.expiresAt();
                Instant createdInstant = Instant.ofEpochMilli(expiredAt);
                LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"); //Make configurable!!!
                String expiresAt = time.format(formatter);

                long duration = expiredAt - System.currentTimeMillis();
                String durationString = plugin.formatTime(duration);

                //Displaying the message
                String message = plugin.getConfig().getString("player-punishments-messages.temp-ban-message", "&cYou are temporarily banned for &f%time_left% &cfrom this server.\\n\\n&7Scope: %scope%\\n&7Reason: &f%reason%\\n&7Expires at: &f%expiration_time%\\n\\n&7Ban ID: &f%id%\\n&7Use this ID to appeal.");
                message = plugin.parsePP(player, message);
                message = message
                        .replace("%scope%", getColoredScope(tempBan.scope()))
                        .replace("%reason%", reason)
                        .replace("%time_left%", durationString)
                        .replace("%expiration_time%", expiresAt)
                        .replace("%id%", tempBan.id());
                e.disallow(PlayerLoginEvent.Result.KICK_BANNED, LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            }
            return;
        }

        e.allow();
    }
    private String getColoredScope(PunishmentScopes scope){
        return switch(scope){
            case DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
            case MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
        };
    }
    private boolean isPunishmentExpired(Punishment p){
        return p.expiresAt() <= System.currentTimeMillis();
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) throws SQLException{
        //This event should only fire when the discord bot is toggled
        boolean toggleDcBot = plugin.botFile().getConfig().getBoolean("toggle-discord-bot", false);
        if(!toggleDcBot) return;

        Player player = e.getPlayer();

        //Check if the player is perm muted (with scope minecraft/global)
        Punishment permMute = plugin.getPlayerPunishmentDataCache().getPunishment(player.getUniqueId(), PunishmentType.PERM_MUTE);
        if(permMute != null && (permMute.scope() == PunishmentScopes.MINECRAFT || permMute.scope() == PunishmentScopes.GLOBAL)){
            if(permMute.active()){
                e.setCancelled(true);

                //Getting the reason and scope
                String reason = permMute.reason();
                String scope = getColoredScope(permMute.scope());

                //Sending the message
                List<String> message = plugin.getConfig().getStringList("player-punishments-messages.perm-mute-message");
                for(String messageLine: message){
                    messageLine = plugin.parsePP(player, messageLine);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                            .replace("%scope%", scope)
                            .replace("%reason%", reason)
                            .replace("%id%", permMute.id())
                    ));
                }
            }
        }

        //Check if the player is temp muted
        Punishment tempMute = plugin.getPlayerPunishmentDataCache().getPunishment(player.getUniqueId(), PunishmentType.TEMP_MUTE);
        if(tempMute != null && (tempMute.scope() == PunishmentScopes.MINECRAFT || tempMute.scope() == PunishmentScopes.GLOBAL)){
            if(isPunishmentExpired(tempMute)) {
                plugin.getDatabaseManager().expirePunishmentById(tempMute.id());
                plugin.getPlayerPunishmentDataCache().expirePunishmentById(player.getUniqueId(), tempMute.id());
            }
            else{
                e.setCancelled(true);

                //Sends the player a message
                //Getting the reason
                String reason = tempMute.reason();

                //Getting the scope
                String scope =  getColoredScope(tempMute.scope());

                //Getting the durations
                long expiresAt = tempMute.expiresAt();
                Instant expiresAtInstant = Instant.ofEpochMilli(expiresAt);
                LocalDateTime time = LocalDateTime.ofInstant(expiresAtInstant, ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"); //Make configurable!!!
                String expiresAtString = time.format(formatter);

                long timeLeft = expiresAt - System.currentTimeMillis();
                String timeLeftString =  plugin.formatTime(timeLeft);

                //Sends the message to the player
                List<String> message = plugin.getConfig().getStringList("player-punishments-messages.temp-mute-message");
                for(String messageLine : message){
                    messageLine = plugin.parsePP(player, messageLine);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageLine
                            .replace("%scope%", scope)
                            .replace("%reason%", reason)
                            .replace("%time_left%", timeLeftString)
                            .replace("%expiration_time%", expiresAtString)
                            .replace("%id%", tempMute.id())
                    ));
                }
            }
        }
    }
}
