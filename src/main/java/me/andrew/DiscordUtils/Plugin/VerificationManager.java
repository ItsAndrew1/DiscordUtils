//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import me.andrew.DiscordUtils.Caching.VerificationCodesCaching;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class VerificationManager{
    private final DiscordUtils plugin;

    public VerificationManager(DiscordUtils plugin){
        this.plugin = plugin;
    }

    public void verificationProcess(Player player) throws SQLException {
        Sound invalid = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.villager.no"));
        assert invalid != null;

        UUID UUID = player.getUniqueId();
        VerificationCodesCaching codes = plugin.getVerificationCodesCaching();

        //Check if the player is already verified
        if(plugin.getVerifiedPlayers().contains(UUID)){
            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("player-is-already-verified-message", "&cYou are already verified!"));
                player.sendMessage(message);
                player.playSound(player.getLocation(), invalid, 1f, 1f);
            });
            return;
        }

        //Check if the player is already verifying
        if(codes.isPlayerVerifying(UUID)){
            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = plugin.getConfig().getString("player-already-verifying-message", "&cPlease finish this verification before starting a new one!");
                message = PlaceholderAPI.setPlaceholders(player, message);
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
                player.playSound(player.getLocation(), invalid, 1f, 1f);
            });
            return;
        }

        //Checking if the code expired in the meantime
        if(codes.getCode(UUID) == null) {
            codes.deleteCode(UUID);

            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = plugin.getConfig().getString("code-expired-message", "&cLast Verification code expired.");
                message = PlaceholderAPI.setPlaceholders(player, message);
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            });
        }

        String verificationCode = getVerificationCode();
        long durationSeconds = plugin.getConfig().getLong("verification-code-expire-time");
        long expireTime = System.currentTimeMillis() + durationSeconds*1000L;

        //Saving the code with the expire time
        codes.createCode(UUID, verificationCode, expireTime);

        //Sends the player a message and sound
        Bukkit.getScheduler().runTask(plugin, () -> {
            long durationMinutes = durationSeconds/60;
            float gvcsVolume = plugin.getConfig().getInt("gvcs-volume");
            float gvcsPitch = plugin.getConfig().getInt("gvcs-pitch");

            Sound giveVerificationCodeSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("give-verification-code-sound", "block.note_block.pling").toLowerCase()));
            player.playSound(player.getLocation(), giveVerificationCodeSound, gvcsVolume, gvcsPitch);
            List<String> chatMessage = plugin.getConfig().getStringList("use-verification-code-message");
            for(String line : chatMessage){
                String parsedLine = line
                        .replace("%code%", verificationCode)
                        .replace("%expire_at%", String.valueOf(durationMinutes));
                parsedLine = plugin.parsePP(player, parsedLine);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', parsedLine));
            }
        });
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
