//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class SlashCommands extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final BotMain botMain;

    public SlashCommands(DiscordUtils plugin, BotMain botMain){
        this.plugin = plugin;
        this.botMain = botMain;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        FileConfiguration botConfig = plugin.botFile().getConfig();

        switch (event.getName()) {
            //The 'verify' command
            case "verify" -> {
                String userId = event.getUser().getId();
                int verificationCode = event.getOption("code").getAsInt();
                UUID uuid;
                try { //Getting the UUID
                    uuid = plugin.getDatabaseManager().getUuidFromCode(verificationCode);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Checking if the user is already verified
                if(plugin.getVerifiedPlayers().contains(uuid)){
                    event.reply("You are already verified!").setEphemeral(true).queue();
                    return;
                }

                //Checking if the code expired or is invalid
                if (uuid == null) {
                    boolean ephemeral = botConfig.getBoolean("iecm-set-ephemeral");
                    String message = botConfig.getString("invalid-expired-code-message", "**Invalid** or **expired** verification code!");
                    event.reply(message).setEphemeral(ephemeral).queue();
                    return;
                }

                Player player = Bukkit.getPlayer(uuid);
                assert player != null;

                try {
                    plugin.getDatabaseManager().setPlayerVerified(uuid, userId);
                    plugin.getDatabaseManager().deleteExpiredCode(uuid);

                    //Adding the player to the Verified Players map
                    plugin.getVerifiedPlayers().add(uuid);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        //Sends a message
                        List<String> hasVerifiedMessage = plugin.getConfig().getStringList("player-verified-message");
                        for(String line : hasVerifiedMessage){
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                        }

                        //Assigns the Verified role to the user and removed the Unverified one
                        Guild dcServer = plugin.getDiscordBot().getDiscordServer();
                        dcServer.retrieveMemberById(userId).queue(member -> {
                            long verifiedRoleID = plugin.botFile().getConfig().getLong("verification.verified-role-id");
                            long unverifiedRoleID = plugin.botFile().getConfig().getLong("verification.unverified-role-id");
                            Role unverifiedRole = dcServer.getRoleById(unverifiedRoleID);
                            Role verifiedRole = dcServer.getRoleById(verifiedRoleID);
                            dcServer.addRoleToMember(member, verifiedRole).queue();
                            dcServer.removeRoleFromMember(member, unverifiedRole).queue();
                        });

                        //Sound
                        Sound hasVerifiedSound =  Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-has-verified-sound", "entity.player.levelup").toLowerCase()));
                        float phvsVolume = plugin.getConfig().getInt("phvs-volume");
                        float phvsPitch = plugin.getConfig().getInt("phvs-pitch");
                        player.playSound(player.getLocation(), hasVerifiedSound, phvsVolume, phvsPitch);

                        //Giving the rewards if there are any (and if rewards are toggled)
                        boolean toggleRewards = plugin.getConfig().getBoolean("rewards.toggle-giving-rewards", false);
                        if(toggleRewards) {
                            //Giving exp if the value is over 0
                            int expLevels = plugin.getConfig().getInt("rewards.exp");
                            if (expLevels > 0) player.giveExp(expLevels);

                            //Giving the items
                            ConfigurationSection itemsToGive = plugin.getConfig().getConfigurationSection("rewards.items");
                            if (itemsToGive != null) {
                                for (String stringItem : itemsToGive.getKeys(false)) {
                                    String stringMaterial = plugin.getConfig().getString("rewards.items." + stringItem + ".material");
                                    int itemQuantity = plugin.getConfig().getInt("rewards.items." + stringItem + ".quantity");
                                    ItemStack item;
                                    try {
                                        item = new ItemStack(Material.matchMaterial(stringMaterial.toUpperCase()), itemQuantity);
                                    } catch (Exception e) {
                                        String errorMessage = plugin.getConfig().getString("error-giving-rewards-message");
                                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                                        Bukkit.getLogger().warning("[DISCORDUTILS] One/More reward item(s) are invalid! Giving rewards won't work!");
                                        Bukkit.getLogger().warning("[DISCORDUTILS] " + e.getMessage());
                                        return;
                                    }

                                    //Attaching the enchants to the item
                                    ConfigurationSection itemEnchants = plugin.getConfig().getConfigurationSection("rewards.items." + stringItem + ".enchantments");
                                    if (itemEnchants != null) {
                                        for (String enchantmentString : itemEnchants.getKeys(false)) {
                                            try {
                                                Enchantment enchant = Enchantment.getByName(enchantmentString);
                                                int enchantLevel = plugin.getConfig().getInt("rewards.items." + stringItem + ".enchantments." + enchantmentString);
                                                item.addEnchantment(enchant, enchantLevel);
                                            } catch (Exception e) {
                                                String errorMessage = plugin.getConfig().getString("error-giving-rewards-message");
                                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                                                Bukkit.getLogger().warning("[DISCORDUTILS] One/More enchantment(s) for item " + stringItem + " are invalid! Giving rewards won't work!");
                                                Bukkit.getLogger().warning("[DISCORDUTILS] " + e.getMessage());
                                                return;
                                            }
                                        }
                                    }

                                    //Drops the rewards if the player doesn't have enough inv space
                                    if (player.getInventory().firstEmpty() == -1) {
                                        World playerWorld = player.getWorld();
                                        double playerX = player.getLocation().getX();
                                        double playerY = player.getLocation().getY();
                                        double playerZ = player.getLocation().getZ();
                                        Location dropLocation = new Location(playerWorld, playerX + 1, playerY, playerZ); //Drop them in front of him

                                        playerWorld.dropItem(dropLocation, item);
                                    } else player.getInventory().addItem(item);
                                }
                            }
                        }
                    });

                    //Setting the user's nickname after their MC ign
                    botMain.getDiscordServer().retrieveMemberById(userId).queue(member -> {
                        if(!member.isOwner()) member.modifyNickname(player.getName()).queue();
                    });

                    String message = botConfig.getString("player-verified-message", "✅ You are now verified! Have fun on our server!");
                    boolean ephemeral = botConfig.getBoolean("pvm-set-ephemeral", true);
                    event.reply(message).setEphemeral(ephemeral).queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            //pshistory command
            case "pshistory" -> {
                event.deferReply(true).queue();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try{
                        //Checking if the user is banned
                        if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                            event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                            return;
                        }

                        //Check if the user is verified
                        String userPlayerIGN = getUserPlayerIGN(event.getUser().getId());
                        if(userPlayerIGN == null){
                            event.getHook().sendMessage("You **are not** verified! Please run */verify* on our server and try again.").queue();
                            return;
                        }

                        OfflinePlayer userPlayer = Bukkit.getOfflinePlayer(userPlayerIGN);
                        UUID userPlayerUUID = userPlayer.getUniqueId();

                        if(event.getOption("ign") == null){
                            boolean playerHasPunishments = plugin.getDatabaseManager().playerHasPunishments(userPlayerUUID);

                            if(!playerHasPunishments){
                                event.getHook().sendMessage("You **do not have** any punishments yet!").queue();
                                return;
                            }

                            botMain.getPunishmentHistory().displayPunishments(event, userPlayerUUID, PunishmentsFilter.ALL, true);
                            return;
                        }

                        //Checking if the user has permission to check the history of others
                        boolean hasPermission = false;
                        List<Long> psRemoveRoles = botConfig.getLongList("pshistory-cmd-roles");
                        if(event.getMember() != null){
                            for(Long roleID : psRemoveRoles){
                                Role role = botMain.getDiscordServer().getRoleById(roleID);
                                if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                            }
                        }
                        if(!hasPermission){
                            event.getHook().sendMessage("You don't have permission to use this command!").queue();
                            return;
                        }

                        //Getting the target player
                        String ign = event.getOption("ign").getAsString();
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);
                        UUID targetPlayerUUID = targetPlayer.getUniqueId();
                        String targetPlayerName = targetPlayer.getName();

                        //Check if the target player has any punishments
                        boolean playerHasPunishments = plugin.getDatabaseManager().playerHasPunishments(targetPlayerUUID);
                        if(!playerHasPunishments){
                            event.getHook().sendMessage("Player **"+targetPlayerName+"** does not have any punishments yet!").queue();
                            return;
                        }

                        botMain.getPunishmentHistory().displayPunishments(event, targetPlayerUUID, PunishmentsFilter.ALL, false);
                    } catch (SQLException e){
                        throw new RuntimeException(e);
                    }
                });
            }

            case "punish" -> {
                event.deferReply(true).queue();

                //Checking if the user is banned
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try{
                        if(isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL) || isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD)){
                            event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                            return;
                        }

                        //Checking if the user has the necessary roles
                        boolean hasPermission = false;
                        List<Long> psRemoveRoles = botConfig.getLongList("punish-cmd-roles");
                        for(Long roleID : psRemoveRoles){
                            Role role = botMain.getDiscordServer().getRoleById(roleID);
                            if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                        }
                        if(!hasPermission){
                            event.getHook().sendMessage("You don't have permission to use this command!").queue();
                            return;
                        }

                        //Getting the player from the ign
                        String ign = event.getOption("ign").getAsString();
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);

                        if (targetPlayer == Bukkit.getOfflinePlayer(getUserPlayerIGN(event.getUser().getId()))) {
                            event.getHook().sendMessage("You cannot punish yourself!").queue();
                            return;
                        }

                        //Check if the target player has played on the server
                        if (!targetPlayer.hasPlayedBefore()) {
                            event.getHook().sendMessage("Player **\\" + targetPlayer.getName() + "** does not exist on the server. Please enter a valid name!").queue();
                            return;
                        }

                        botMain.getAddPunishments().punishPlayer(event, targetPlayer);
                    } catch (SQLException e){
                        throw new RuntimeException(e);
                    }
                });
            }

            case "psremove" -> {
                event.deferReply(true).queue();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                   try{
                       //Checking if the user is banned
                       if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                           event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                           return;
                       }

                       //Checking if the user has the necessary roles
                       boolean hasPermission = false;
                       List<Long> psRemoveRoles = botConfig.getLongList("psremove-cmd-roles");
                       for(Long roleID : psRemoveRoles){
                           Role role = botMain.getDiscordServer().getRoleById(roleID);
                           if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                       }
                       if(!hasPermission){
                           event.getHook().sendMessage("You don't have permission to use this command!").queue();
                           return;
                       }

                       //Getting the ID from the command
                       String ID = event.getOption("id").getAsString();

                       //Checking if there is a punishment with that ID
                       if(!punishmentExists(ID)){
                           event.getHook().sendMessage("There is **no punishment** with that ID!").queue();
                           return;
                       }

                       //Expiring the punishment that has the typed ID
                       Connection dbConnection = plugin.getDatabaseManager().getConnection();
                       String SQL = "UPDATE punishments SET active = false, removed = true, removed_at = ? WHERE id = ?";

                       PreparedStatement ps = dbConnection.prepareStatement(SQL);
                       ps.setLong(1, System.currentTimeMillis());
                       ps.setString(2, ID);
                       ps.executeUpdate();

                       //Getting the punishment scope and type
                       String SQL2 = "SELECT type, scope, uuid FROM punishments WHERE id = ?";
                       PreparedStatement ps2 = dbConnection.prepareStatement(SQL2);
                       ps2.setString(1, ID);
                       ResultSet rs = ps2.executeQuery();

                       if(rs.next()){
                           PunishmentScopes scope = PunishmentScopes.valueOf(rs.getString("scope"));
                           final PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
                           UUID targetUUID = UUID.fromString(rs.getString("uuid"));

                           //If the scope is Discord or Global, I have to unban/remove the timeout of the user
                           if(scope == PunishmentScopes.DISCORD || scope == PunishmentScopes.GLOBAL){
                               Guild dcServer = botMain.getDiscordServer();

                               botMain.getJda().retrieveUserById(getTargetPlayerUserID(targetUUID)).queue(targetUser -> {
                                   if(type == PunishmentType.PERM_BAN || type == PunishmentType.TEMP_BAN){
                                       //Removes the 'banned' role (and give him the 'verified' role) from the member if he has it
                                       dcServer.retrieveMemberById(targetUser.getId()).queue(member -> {
                                           long bannedRoleID = plugin.botFile().getConfig().getLong("ban-role-id");
                                           Role bannedRole = dcServer.getRoleById(bannedRoleID);

                                           long verifiedRoleID = plugin.botFile().getConfig().getLong("verification.verified-role-id");
                                           Role verifiedRole = botMain.getDiscordServer().getRoleById(verifiedRoleID);
                                           if(member.getRoles().contains(bannedRole)){
                                               dcServer.removeRoleFromMember(member, bannedRole).queue();
                                               dcServer.addRoleToMember(member, verifiedRole).queue();
                                           }
                                       });
                                   }

                                   if(type == PunishmentType.PERM_MUTE || type == PunishmentType.TEMP_MUTE){
                                       //Removes the timeout role from the member if he has it
                                       dcServer.retrieveMemberById(targetUser.getId()).queue(targetMember -> {
                                           long timeoutRoleID = botConfig.getLong("timeout-role-id");
                                           Role timeoutRole = dcServer.getRoleById(timeoutRoleID);
                                           if(targetMember.getRoles().contains(timeoutRole)) dcServer.removeRoleFromMember(targetMember, timeoutRole).queue();
                                       });

                                       if(type == PunishmentType.TEMP_MUTE) dcServer.removeTimeout(targetUser).queue();
                                   }
                               });
                           }
                       }

                       event.getHook().sendMessage("Punishment with ID **"+ID+"** has been removed!").queue();
                   } catch (SQLException e){
                       throw new RuntimeException(e);
                   }
                });
            }

            case "unverify" -> {
                event.deferReply(true).queue();
                String userID = event.getUser().getId();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try{
                        //Checking if the user is banned
                        if(isUserBanned(userID, PunishmentScopes.DISCORD) || isUserBanned(userID, PunishmentScopes.GLOBAL)){
                            event.getHook().sendMessage("You cannot do this because **you are banned**!").queue();
                            return;
                        }

                        //Checking if the user is verified
                        if(!isUserVerified(userID)){
                            event.getHook().sendMessage("You *don't have* any MC account linked to your DC account! Use **/verify** to link one!").queue();
                            return;
                        }

                        Connection dbConnection = plugin.getDatabaseManager().getConnection();
                        String sql = "DELETE FROM playersVerification WHERE discordId = ?";

                        try(PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                            //Removing the user from the playersVerification table
                            ps.setString(1, userID);
                            ps.executeUpdate();

                            //Removing from the user the 'Verified' role and giving him the Unverified role
                            long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
                            Role verifiedRole = botMain.getDiscordServer().getRoleById(verifiedRoleID);

                            Member targetMember = event.getMember();
                            assert targetMember != null;

                            if (targetMember.getRoles().contains(verifiedRole)) botMain.getDiscordServer().removeRoleFromMember(targetMember, verifiedRole).queue();

                            String unverifiedRoleID = botConfig.getString("verification.unverified-role-id");
                            Role unverified = botMain.getDiscordServer().getRoleById(unverifiedRoleID);
                            botMain.getDiscordServer().addRoleToMember(targetMember, unverified).queue();

                            //Resetting the nickname
                            if (!targetMember.isOwner()) targetMember.modifyNickname(null).queue();

                            //Removing the player from the Verified Players maps
                            plugin.getVerifiedPlayers().remove(Bukkit.getOfflinePlayer(getUserPlayerIGN(userID)).getUniqueId());

                            event.getHook().sendMessage("Unverified successfully!").queue();
                        }
                    } catch (SQLException e){
                        throw new RuntimeException(e);
                    }
                });
            }

            case "appeal" -> {
                String punishmentID = event.getOption("id").getAsString();

                //Opening a modal with the appealing form
                int maximumLength = botConfig.getInt("maximum-length-appeal");
                int minimumLength = botConfig.getInt("minimum-length-appeal");
                String placeholder = botConfig.getString("placeholder-appeal");

                TextInput reasonForm = TextInput.create("reasonForm", TextInputStyle.PARAGRAPH)
                        .setPlaceholder(placeholder)
                        .setRequired(true)
                        .setMinLength(minimumLength)
                        .setMaxLength(maximumLength).build();

                Modal formModal = Modal.create("appeal_form:"+punishmentID, "Appeal Your Punishment")
                        .addComponents(Label.of("Form", reasonForm))
                        .build();

                event.replyModal(formModal).queue();
            }

            case "appealstatus" -> {
                event.deferReply(true).queue();

                //Getting the punishment ID
                String pID = event.getOption("id").getAsString();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        //If the status is 'PENDING'
                        if(isPunishmentInPendingState(pID)) event.getHook().sendMessage("Punishment ID: "+pID+"\n**Status**: PENDING").queue();

                        //If the status is 'DECLINED'
                        if(wasAppealDeclined(pID)) event.getHook().sendMessage("Punishment ID: "+pID+"\n**Status**: DECLINED").queue();

                        //If the status is 'ACCEPTED'
                        if(wasAppealAccepted(pID)) event.getHook().sendMessage("Punishment ID: "+pID+"\n**Status**: ACCEPTED").queue();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            case "startverification" -> {
                //Checking if the user is banned
                try {
                    if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                        event.reply("You cannot do this because **you are banned**!").setEphemeral(true).queue();
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Checking if the user has permission
                boolean hasPermission = false;
                List<Long> psRemoveRoles = botConfig.getLongList("RefreshVerification-cmd-roles");
                for(Long roleID : psRemoveRoles){
                    Role role = botMain.getDiscordServer().getRoleById(roleID);
                    if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                }
                if(!hasPermission){
                    event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                event.getGuild().loadMembers().onSuccess(members -> {
                    for(Member member : members){
                        //Skipping the bot and the owner.
                        if(member.getUser().isBot()) continue;
                        if(member.isOwner()) continue;

                        //Removing the roles from the list (if the member has)
                        List<Long> roleIdList = botConfig.getLongList("roles-to-be-deleted");
                        for(Long roleID : roleIdList){
                            Role roleFromList = event.getGuild().getRoleById(roleID);
                            if(member.getRoles().contains(roleFromList)) event.getGuild().removeRoleFromMember(member, roleFromList).queue();
                        }

                        //Giving the unverified role
                        long unverifiedRoleID = botConfig.getLong("verification.unverified-role-id");
                        Role unverifiedRole = event.getGuild().getRoleById(unverifiedRoleID);
                        event.getGuild().addRoleToMember(member, unverifiedRole).queue();
                    }
                });

                event.reply("Verification Process started!").setEphemeral(true).queue();
            }

            case "reload" -> {
                event.deferReply(true).queue();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try{
                        //Checking if the user is banned
                        if(isUserBanned(event.getUser().getId(), PunishmentScopes.DISCORD) || isUserBanned(event.getUser().getId(), PunishmentScopes.GLOBAL)){
                            event.reply("You cannot do this because **you are banned**!").setEphemeral(true).queue();
                            return;
                        }

                        //Checking if the user has permission
                        boolean hasPermission = false;
                        List<Long> psRemoveRoles = botConfig.getLongList("RefreshVerification-cmd-roles");
                        for(Long roleID : psRemoveRoles){
                            Role role = botMain.getDiscordServer().getRoleById(roleID);
                            if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                        }
                        if(!hasPermission){
                            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
                            return;
                        }

                        event.reply("Bot is restarting...").setEphemeral(true).queue();

                        //Reloading the config file
                        plugin.botFile().reloadConfig();

                        //Restarting the bot
                        botMain.getJda().shutdownNow();

                        try {
                            String token = botConfig.getString("bot-token");
                            String serverID = botConfig.getString("guild-id");
                            new BotMain(token, serverID, plugin);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Couldn't reload the bot! See message: "+e.getMessage());
                        }
                    }catch(SQLException e){
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private boolean isUserVerified(String ID) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String SQL = "SELECT 1 FROM playersVerification WHERE discordId = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(SQL)){
            ps.setString(1, ID);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    private String getUserPlayerIGN(String userId) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        try(PreparedStatement ps = dbConnection.prepareStatement("SELECT ign FROM playersVerification WHERE discordId = ?")){
            ps.setString(1, userId);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return rs.getString("ign");
            }
        }

        return null;
    }

    private String getTargetPlayerUserID(UUID targetUUID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String SQL = "SELECT discordId FROM playersVerification WHERE uuid = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(SQL)){
            ps.setString(1, targetUUID.toString());
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return null;

                return rs.getString("discordId");
            }
        }
    }

    private boolean punishmentExists(String ID) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT 1 FROM punishments WHERE id = ? AND active = 1";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }
    }

    private boolean isPunishmentInPendingState(String punishmentID) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT appeal_state FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, punishmentID);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;

                return rs.getString("appeal_state").equals("pending");
            }
        }
    }

    private boolean wasAppealAccepted(String ID) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT appeal_state FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;
                String result = rs.getString("appeal_state");
                return result.equals("accepted");
            }
        }
    }

    private boolean wasAppealDeclined(String ID) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        String sql = "SELECT appeal_state FROM punishments WHERE id = ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, ID);
            try(ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return false;
                return rs.getString("appeal_state").equals("declined");
            }
        }
    }

    private boolean isUserBanned(String userID, PunishmentScopes scope) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        boolean permBanned = false, tempBanned = false;
        String sql = "SELECT 1 FROM punishments WHERE uuid = ? AND type = ? AND scope = ? AND active = 1";

        //Checking if the user is permanently banned
        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, userID);
            ps.setString(2, PunishmentType.PERM_BAN.name());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) permBanned = true;
            }
        }

        //Checking if the user is temporarily banned
        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, userID);
            ps.setString(2, PunishmentType.TEMP_BAN.name());
            ps.setString(3, scope.name());
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) tempBanned = true;
            }
        }

        return permBanned || tempBanned;
    }
}
