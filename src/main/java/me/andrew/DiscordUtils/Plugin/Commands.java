//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Commands implements CommandExecutor{
    private final DiscordUtils plugin;

    public Commands(DiscordUtils plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) sender;
        String chatPrefix = plugin.getConfig().getString("chat-prefix");

        try {
            //Check if the player is muted temporarily
            Punishment tempMute = plugin.getDatabaseManager().getPunishment(player.getUniqueId(), PunishmentType.TEMP_MUTE);
            if(tempMute != null && (tempMute.getScope() == PunishmentScopes.MINECRAFT || tempMute.getScope() == PunishmentScopes.GLOBAL)){
                if(isPunishmentExpired(tempMute)) plugin.getDatabaseManager().expirePunishmentById(tempMute.getCrt());
                else{
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
                    return true;
                }
            }

            //Check if the player is muted permanently
            Punishment permMute = plugin.getDatabaseManager().getPunishment(player.getUniqueId(), PunishmentType.PERM_MUTE);
            if(permMute != null && (permMute.getScope() == PunishmentScopes.MINECRAFT || permMute.getScope() == PunishmentScopes.GLOBAL)){
                if(permMute.isActive()){
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
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //Checking if the player has permission for commands
        if(!player.hasPermission("discordutils.commands")){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou are not allowed to use commands!"));
            return true;
        }

        String commandNoPermissionMessage = plugin.getConfig().getString("command-no-permission-message");

        //Get the sounds
        Sound good = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
        Sound invalid = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));

        if(command.getName().equalsIgnoreCase("dcutils")){
            if(strings.length == 0){
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cUsage: &l/dcutils <blockConfig | punishments | reload | help>"));
                return true;
            }

            switch(strings[0]){
                case "mainconfig":
                    if(!player.hasPermission("discordutils.commands.mainconfig")){
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', commandNoPermissionMessage));
                        return true;
                    }

                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    plugin.getMainConfigGUI().showGUI(player);
                    break;

                case "punishments":
                    if(!player.hasPermission("discordutils.commands.punishments")){
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', commandNoPermissionMessage));
                        return true;
                    }

                    //Checking if the database is setup properly
                    if(plugin.getDatabaseManager() == null){
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou cannot do this since the database didn't setup properly!"));
                        return true;
                    }

                    try {
                        plugin.getPlayerHeadsGUIs().showGui(player, 1);
                        player.playSound(player.getLocation(), good, 1f, 1.4f);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case "help":
                    if(!player.hasPermission("discordutils.commands.help")){
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', commandNoPermissionMessage));
                        return true;
                    }

                    List<String> helpBookPages = plugin.getConfig().getStringList("help-message-book-pages");
                    ItemStack helpBook = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta hbMeta = (BookMeta) helpBook.getItemMeta();

                    hbMeta.setTitle(ChatColor.translateAlternateColorCodes('&', "&l&9DiscordUtils Help"));
                    hbMeta.setAuthor(ChatColor.translateAlternateColorCodes('&', "&l&b_ItsAndrew_"));

                    //Adds the pages from config to the help book
                    for(String page : helpBookPages){
                        String coloredPage = ChatColor.translateAlternateColorCodes('&', page);
                        hbMeta.addPage(coloredPage);
                    }

                    helpBook.setItemMeta(hbMeta);
                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    player.openBook(helpBook);
                    break;

                case "reload":
                    if(!player.hasPermission("discordutils.commands.reload")){
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', commandNoPermissionMessage));
                        return true;
                    }

                    plugin.reloadConfig();
                    plugin.getDiscordBlockManager().spawnDiscordBlock(); //Spawns the discord block
                    if(plugin.getDiscordBlockManager().getParticleTask() != null) plugin.getDiscordBlockManager().getParticleTask().cancel();
                    plugin.getDiscordBlockManager().startParticleTask(); //Spawns the particles

                    //Starts the broadcastingTask with a new one, so the task's don't pile up
                    if(!plugin.getBroadcastTask().isCancelled()){
                        plugin.getBroadcastTask().cancel();
                        plugin.startBroadcasting();
                    }

                    player.playSound(player.getLocation(), good, 1f, 1.4f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &a&lDiscordUtils &asuccessfully reloaded!"));
                    break;

                default:
                    player.playSound(player.getLocation(), invalid, 1f, 1f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cInvalid command. Use &l/dcutils help &cfor info!"));
                    break;
            }
            return true;
        }

        if(command.getName().equalsIgnoreCase("discord")){
            if(!player.hasPermission("discordutils.discord")){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-no-permission-message")));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                return true;
            }

            plugin.getDiscordGUI().showGUI(player);
            return true;
        }

        if(command.getName().equalsIgnoreCase("verify")){
            //TO DO: Check if the bot is toggled or not

            if(!player.hasPermission("discordutils.verify")){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-no-permission-message")));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                return true;
            }
            try {
                plugin.getVerificationManager().verificationProcess(player);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        if(command.getName().equalsIgnoreCase("unverify")){
            //TO DO: Same as 'verify'

            //Checking if the player has permission to run the command
            if(!player.hasPermission("discordutils.unverify")){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&' , plugin.getConfig().getString("command-no-permission-message")));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                return true;
            }

            FileConfiguration botConfig = plugin.botFile().getConfig();

            //Checking if the player is verified
            if(!plugin.getPlayerVerificationCache().isPlayerVerifiedUUID(player.getUniqueId())){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have a MC account linked to the DC server. Run &l/verify &cto link one!"));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                return true;
            }

            //Removing the 'Verified' role from the target user and giving him Unverified role
            Guild dcServer = plugin.getDiscordBot().getDiscordServer();

            long unverifiedRoleID = botConfig.getLong("verification.unverified-role-id");
            Role unverified = dcServer.getRoleById(unverifiedRoleID);
            long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
            Role verifiedRole = dcServer.getRoleById(verifiedRoleID);

            String userDiscordID = plugin.getPlayerVerificationCache().getDiscordIdFromUuid(player.getUniqueId());
            dcServer.retrieveMemberById(userDiscordID).queue(targetMember -> {
                //Removing the 'verified' role and giving himt he 'unverified' role
                if(targetMember.getRoles().contains(verifiedRole)) dcServer.removeRoleFromMember(targetMember, verifiedRole).queue();
                dcServer.addRoleToMember(targetMember, unverified).queue();

                //Resetting the user's nickname
                if(!targetMember.isOwner()) targetMember.modifyNickname(null).queue();
            });

            //Attempts to send the user a message about this.
            plugin.getDiscordBot().getJda().retrieveUserById(userDiscordID).queue(targetUser -> {
                targetUser.openPrivateChannel().queue(privateChannel -> {
                    String message = plugin.botFile().getConfig().getString("unverified-from-mc-DM", "You have unverified from **%server_name%**. To verify again, run **/verify** on their Minecraft Server!");
                    privateChannel.sendMessage(message).queue();
                });
            });

            String message = plugin.getConfig().getString("unverified-from-mc-message", "&4[&c&l!&4] &aYou have been unverified! Run &e&l/verify&a in order to verify again.");
            message = PlaceholderAPI.setPlaceholders(player, message);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));

            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-unverified-sound", "entity.player.levelup").toLowerCase()));
            float volume = plugin.getConfig().getInt("pus-volume", 1);
            float pitch = plugin.getConfig().getInt("pus-pitch", 1);
            player.playSound(player.getLocation(), sound, volume, pitch);

            //Removing the player from the playersVerification table
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql = "DELETE FROM verifiedPlayers WHERE uuid = ?";
                try (Connection conn = plugin.getDatabaseManager().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, player.getUniqueId().toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            //Removing from the UUID <-> DiscordId and vice versa maps
            String playerDiscordId = plugin.getPlayerVerificationCache().getDiscordIdFromUuid(player.getUniqueId());
            plugin.getPlayerVerificationCache().removeDiscordIdUUID(playerDiscordId);
            plugin.getPlayerVerificationCache().removeUuidDiscordID(player.getUniqueId());
            return true;
        }

        if(command.getName().equalsIgnoreCase("history")){
            //Checking if the player has the permission
            if(!player.hasPermission("discordutils.viewhistory")){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&' , plugin.getConfig().getString("command-no-permission-message")));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                return true;
            }

            try {
                plugin.getPunishmentsGUI().showGui(player, 1, true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    private boolean isPunishmentExpired(Punishment p){
        return p.getExpiresAt() <= System.currentTimeMillis();
    }
    private String getColoredScope(PunishmentScopes scope){
        return switch(scope){
            case DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
            case MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
        };
    }
}
