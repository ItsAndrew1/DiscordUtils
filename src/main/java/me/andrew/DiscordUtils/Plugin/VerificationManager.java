//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class VerificationManager{
    private final DiscordUtils plugin;

    public VerificationManager(DiscordUtils plugin){
        this.plugin = plugin;
    }

    public void verificationProcess(Player player) throws SQLException {
        //Inserts the player into the playersVerification table if the doesn't exit already
        if(!plugin.getDatabaseManager().playerAlreadyExits(player.getUniqueId())){
            try(PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("INSERT INTO playersVerification (uuid, discordId, verified, hasVerified) values (?, null, false, false)")){
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
            }
        }
        Sound invalid = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.villager.no"));

        //Check if the player has verified
        if(plugin.getDatabaseManager().hasPlayerVerified(player.getUniqueId())){
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCongrats! You have officially been verified. Meriti un cartof."));

            //Deletes the hasVerified boolean from the table
            try(PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("UPDATE playersVerification SET hasVerified=null WHERE uuid = ?")){
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
            }
            return;
        }

        //Check if the player has been verified
        if(plugin.getDatabaseManager().isVerified(player.getUniqueId())){
            String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-is-already-verified-message"));
            player.sendMessage(message);
            player.playSound(player.getLocation(), invalid, 1f, 1f);
            return;
        }

        //Check if the code expired
        if(plugin.getDatabaseManager().isCodeExpired(player.getUniqueId())){
            plugin.getDatabaseManager().deleteExpiredCode(player.getUniqueId());
            String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("code-expired-message"));
            player.sendMessage(message);
            player.playSound(player.getLocation(), invalid, 1f, 1f);
            return;
        }

        //Check if the player is already verifying
        if(plugin.getDatabaseManager().isPlayerVerifying(player.getUniqueId())){
            String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-already-verifying-message"));
            player.sendMessage(message);
            player.playSound(player.getLocation(), invalid, 1f, 1f);
            return;
        }

        //Creates and stores the verification code in the db
        String verificationCode = getVerificationCode();
        long durationSeconds = plugin.getConfig().getLong("verification-code-expire-time");
        long expireTime = System.currentTimeMillis() + durationSeconds*1000L;
        try(PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("INSERT INTO verificationCodes(uuid, code, expire_at) values (?, ?, ?)")){
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, verificationCode);
            ps.setLong(3, expireTime);
            ps.executeUpdate();
        }

        //Sends the player a message and sound
        long durationMinutes = durationSeconds/60;
        Sound giveVerificationCodeSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("give-verification-code-sound").toLowerCase()));
        float gvcsVolume = plugin.getConfig().getInt("gvcs-volume");
        float gvcsPitch = plugin.getConfig().getInt("gvcs-pitch");

        player.playSound(player.getLocation(), giveVerificationCodeSound, gvcsVolume, gvcsPitch);
        List<String> chatMessage = plugin.getConfig().getStringList("use-verification-code-message");
        for(String line : chatMessage){
            String parsedLine = line
                    .replace("%code%", verificationCode)
                    .replace("%expire_at%", String.valueOf(durationMinutes));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', parsedLine));
        }
    }

    private String getVerificationCode(){
        int codeLength = plugin.getConfig().getInt("verification-code-length");
        StringBuilder verificationCode = new StringBuilder(codeLength);
        SecureRandom random = new SecureRandom();

        for(int i = 0; i < codeLength; i++){
            verificationCode.append(random.nextInt(10)); //Generates everytime a number from 0-9
        }

        return verificationCode.toString();
    }
}
