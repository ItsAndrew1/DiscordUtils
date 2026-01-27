package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
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
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.awt.Color;
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
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
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
                Connection dbConnection = plugin.getDatabaseManager().getConnection();
                String sql = "SELECT 1 FROM playersVerification WHERE discordId = ?";
                try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                    ps.setString(1, userId);
                    ResultSet rs = ps.executeQuery();

                    if(rs.next()){
                        event.reply("You are already verified!").setEphemeral(true).queue();
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Checking if the code expired or is invalid
                if (uuid == null) {
                    boolean ephemeral = botConfig.getBoolean("iecm-set-ephemeral");
                    String message = botConfig.getString("invalid-expired-code-message");
                    event.reply(message).setEphemeral(ephemeral).queue();
                    return;
                }

                Player player = Bukkit.getPlayer(uuid);
                try {
                    plugin.getDatabaseManager().setPlayerVerified(uuid, userId);
                    plugin.getDatabaseManager().deleteExpiredCode(uuid);

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
                        Sound hasVerifiedSound =  Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-has-verified-sound").toLowerCase()));
                        float phvsVolume = plugin.getConfig().getInt("phvs-volume");
                        float phvsPitch = plugin.getConfig().getInt("phvs-pitch");
                        player.playSound(player.getLocation(), hasVerifiedSound, phvsVolume, phvsPitch);

                        //Giving the rewards if there are any (and if rewards are toggled)
                        boolean toggleRewards = plugin.getConfig().getBoolean("rewards.toggle-giving-rewards");
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

                    String message = botConfig.getString("player-verified-message");
                    boolean ephemeral = botConfig.getBoolean("pvm-set-ephemeral");
                    event.reply(message).setEphemeral(ephemeral).queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            //pshistory command
            case "pshistory" -> {
                OfflinePlayer userPlayer;
                try {
                    userPlayer = getUserPlayer(event.getUser().getId());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Check if the user is verified
                if (userPlayer == null) {
                    event.reply("You **are not** verified! Please run */verify* on our server and try again.").setEphemeral(true).queue();
                    return;
                }

                if (event.getOption("ign") == null) {
                    try {
                        //Check if the user has any punishments
                        if (!plugin.getDatabaseManager().playerHasPunishments(userPlayer.getUniqueId())) {
                            event.reply("You **do not have** any punishments yet!").setEphemeral(true).queue();
                            return;
                        }

                        event.deferReply().setEphemeral(true).queue();
                        botMain.getPunishmentHistory().displayPunishments(event, userPlayer.getUniqueId(), PunishmentsFilter.ALL, true);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                //Checking if the user has permission to check the history of others
                boolean hasPermission = false;
                List<Long> psRemoveRoles = botConfig.getLongList("pshistory-cmd-roles");
                for(Long roleID : psRemoveRoles){
                    Role role = botMain.getDiscordServer().getRoleById(roleID);
                    if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                }
                if(!hasPermission){
                    event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                //Getting the target player
                String ign = event.getOption("ign").getAsString();
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);

                //Check if the target player is the user player
                if (targetPlayer == userPlayer) {
                    event.reply("You **cannot** do this! Use */pshistory* to view **your own** punishments.").setEphemeral(true).queue();
                    return;
                }

                //Checking if the player exists on the server.
                if (!targetPlayer.hasPlayedBefore()) {
                    event.reply("Player " + targetPlayer.getName() + " doesn't exist on this server! Please enter a valid name.").setEphemeral(true).queue();
                    return;
                }

                //Check if the target player has any punishments
                try {
                    if (!plugin.getDatabaseManager().playerHasPunishments(targetPlayer.getUniqueId())) {
                        event.reply("Player **\\" + targetPlayer.getName() + "** does not have any punishments yet!").setEphemeral(true).queue();
                        return;
                    }

                    event.deferReply().setEphemeral(true).queue(); //Getting the interaction hook
                    botMain.getPunishmentHistory().displayPunishments(event, targetPlayer.getUniqueId(), PunishmentsFilter.ALL, false);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            case "punish" -> {
                //Checking if the user has the necessary roles
                boolean hasPermission = false;
                List<Long> psRemoveRoles = botConfig.getLongList("punish-cmd-roles");
                for(Long roleID : psRemoveRoles){
                    Role role = botMain.getDiscordServer().getRoleById(roleID);
                    if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                }
                if(!hasPermission){
                    event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                try {
                    //Getting the player from the ign
                    String ign = event.getOption("ign").getAsString();
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(ign);

                    if (targetPlayer == getUserPlayer(event.getUser().getId())) {
                        event.reply("You cannot punish yourself!").setEphemeral(true).queue();
                        return;
                    }

                    //Check if the target player has played on the server
                    if (!targetPlayer.hasPlayedBefore()) {
                        event.reply("Player **\\" + targetPlayer.getName() + "** does not exist on the server. Please enter a valid name!").setEphemeral(true).queue();
                        return;
                    }

                    botMain.getAddPunishments().punishPlayer(event, targetPlayer);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            case "psremove" -> {
                //Checking if the user has the necessary roles
                boolean hasPermission = false;
                List<Long> psRemoveRoles = botConfig.getLongList("psremove-cmd-roles");
                for(Long roleID : psRemoveRoles){
                    Role role = botMain.getDiscordServer().getRoleById(roleID);
                    if(event.getMember().getRoles().contains(role)) {hasPermission = true; break;}
                }
                if(!hasPermission){
                    event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                try{
                    //Getting the ID from the command
                    String ID = event.getOption("id").getAsString();

                    //Checking if there is a punishment with that ID
                    if(!punishmentExists(ID)){
                        event.reply("There is **no punishment** with that ID!").setEphemeral(true).queue();
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
                                    //Removes the 'banned' role from the member if he has it
                                    dcServer.retrieveMemberById(targetUser.getId()).queue(member -> {
                                        long bannedRoleID = plugin.botFile().getConfig().getLong("ban-role-id");
                                        Role bannedRole = dcServer.getRoleById(bannedRoleID);
                                        if(member.getRoles().contains(bannedRole)) dcServer.removeRoleFromMember(member, bannedRole).queue();
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

                    event.reply("Punishment with ID **"+ID+"** has been removed!").setEphemeral(true).queue();
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            }

            case "unverify" -> {
                String userID = event.getUser().getId();
                Connection dbConnection = plugin.getDatabaseManager().getConnection();
                String sql = "DELETE FROM playersVerification WHERE discordId = ?";

                //Checking if the user is verified
                try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                    if(!isUserVerified(userID)){
                        event.reply("You *don't have* any MC account linked to your DC account! Use **/verify** to link one!").setEphemeral(true).queue();
                        return;
                    }

                    //Removing the user from the playersVerification table
                    ps.setString(1, userID);
                    ps.executeUpdate();

                    //Removing from the user the 'Verified' role and giving him the Unverified role
                    long verifiedRoleID = botConfig.getLong("verification.verified-role-id");
                    Role verifiedRole =  botMain.getDiscordServer().getRoleById(verifiedRoleID);

                    Member targetMember = event.getMember();
                    if(targetMember.getRoles().contains(verifiedRole)) botMain.getDiscordServer().removeRoleFromMember(targetMember, verifiedRole).queue();

                    String unverifiedRoleID = botConfig.getString("verification.unverified-role-id");
                    Role unverified = botMain.getDiscordServer().getRoleById(unverifiedRoleID);
                    botMain.getDiscordServer().addRoleToMember(targetMember, unverified).queue();

                    //Resetting the nickname
                    if(!targetMember.isOwner()) targetMember.modifyNickname(null).queue();

                    event.reply("Unverified successfully!").setEphemeral(true).queue();
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            }

            case "appeal" -> {
                String punishmentID = event.getOption("id").getAsString();

                try {
                    //Checking if the punishment still exists and hasn't expired
                    if(!punishmentExists(punishmentID)){
                        event.reply("This punishment has already been **removed** or has **expired**!").setEphemeral(true).queue();
                        return;
                    }

                    //Checking if the appeal was declined
                    if(wasAppealDeclined(punishmentID)){
                        event.reply("This appeal has been declined. You cannot appeal again.").setEphemeral(true).queue();
                        return;
                    }

                    //Checking if the punishment is already in appeal state
                    if(isPunishmentInPendingState(punishmentID)){
                        event.reply("You have already sent an appeal for this punishment! Please be patient while our staff reviews your appeal.").setEphemeral(true).queue();
                        return;
                    }

                    //Opening a modal with the appealing form
                    int maximumLength = botConfig.getInt("maximum-length-appeal");
                    int minimumLength = botConfig.getInt("minimum-length-appeal");
                    String placeholder = botConfig.getString("placeholder-appeal");

                    TextInput reasonForm = TextInput.create("reasonForm", TextInputStyle.PARAGRAPH)
                            .setPlaceholder(placeholder)
                            .setRequired(true)
                            .setMinLength(minimumLength)
                            .setMaxLength(maximumLength).build();;;;;;;;;;;;;;;;;;
                            ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  //Genta was here ☺
                            ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

                    Modal formModal = Modal.create("appeal_form:"+punishmentID, "Appeal Your Punishment")
                            .addComponents(Label.of("Form", reasonForm))
                            .build();

                    event.replyModal(formModal).queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            case "appealstatus" -> {
                //Getting the punishment ID
                String pID = event.getOption("id").getAsString();

                try {
                    //If the status is 'PENDING'
                    if(isPunishmentInPendingState(pID)) event.reply("Punishment ID: "+pID+"\n**Status**: PENDING").setEphemeral(true).queue();

                    //If the status is 'DECLINED'
                    if(wasAppealDeclined(pID)) event.reply("Punishment ID: "+pID+"\n**Status**: DECLINED").setEphemeral(true).queue();

                    //If the status is 'ACCEPTED'
                    if(wasAppealAccepted(pID)) event.reply("Punishment ID: "+pID+"\n**Status**: ACCEPTED").setEphemeral(true).queue();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
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

    private OfflinePlayer getUserPlayer(String userId) throws SQLException {
        Connection dbConnection = plugin.getDatabaseManager().getConnection();
        OfflinePlayer userPlayer = null;

        try(PreparedStatement ps = dbConnection.prepareStatement("SELECT ign FROM playersVerification WHERE discordId = ?")){
            ps.setString(1, userId);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) userPlayer = Bukkit.getOfflinePlayer(rs.getString("ign"));
            }
        }
        return userPlayer;
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
}
