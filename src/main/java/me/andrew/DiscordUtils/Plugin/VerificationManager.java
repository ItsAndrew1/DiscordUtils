//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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
            //Sends a message
            List<String> hasVerifiedMessage = plugin.getConfig().getStringList("player-verified-message");
            for(String line : hasVerifiedMessage){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }

            //Sound
            Sound hasVerifiedSound =  Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-has-verified-sound").toLowerCase()));
            float phvsVolume = plugin.getConfig().getInt("phvs-volume");
            float phvsPitch = plugin.getConfig().getInt("phvs-pitch");
            player.playSound(player.getLocation(), hasVerifiedSound, phvsVolume, phvsPitch);

            //Giving the rewards if there are any (and if rewards are toggled)
            boolean toggleRewards = plugin.getConfig().getBoolean("rewards.toggle-giving-rewards");
            if(toggleRewards){
                //Giving exp if the value is over 0
                int expLevels = plugin.getConfig().getInt("rewards.exp");
                if(expLevels > 0) player.giveExp(expLevels);

                //Giving the items
                ConfigurationSection itemsToGive = plugin.getConfig().getConfigurationSection("rewards.items");
                if(itemsToGive != null){
                    for(String stringItem : itemsToGive.getKeys(false)){
                        String stringMaterial = plugin.getConfig().getString("rewards.items." + stringItem + ".material");
                        int itemQuantity = plugin.getConfig().getInt("rewards.items." + stringItem + ".quantity");
                        ItemStack item;
                        try{
                            item = new ItemStack(Material.matchMaterial(stringMaterial.toUpperCase()),  itemQuantity);
                        } catch(Exception e){
                            String errorMessage = plugin.getConfig().getString("error-giving-rewards-message");
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                            Bukkit.getLogger().warning("[DISCORDUTILS] One/More reward item(s) are invalid! Giving rewards won't work!");
                            Bukkit.getLogger().warning("[DISCORDUTILS] "+e.getMessage());
                            return;
                        }

                        //Attaching the enchants to the item
                        ConfigurationSection itemEnchants = plugin.getConfig().getConfigurationSection("rewards.items."+stringItem+".enchantments");
                        if(itemEnchants != null){
                            for(String enchantmentString : itemEnchants.getKeys(false)){
                                try{
                                    Enchantment enchant = Enchantment.getByName(enchantmentString);
                                    int enchantLevel = plugin.getConfig().getInt("rewards.items."+stringItem+".enchantments."+enchantmentString);
                                    item.addEnchantment(enchant, enchantLevel);
                                } catch (Exception e){
                                    String errorMessage = plugin.getConfig().getString("error-giving-rewards-message");
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
                                    Bukkit.getLogger().warning("[DISCORDUTILS] One/More enchantment(s) for item "+stringItem+" are invalid! Giving rewards won't work!");
                                    Bukkit.getLogger().warning("[DISCORDUTILS] "+e.getMessage());
                                    return;
                                }
                            }
                        }

                        //Drops the rewards if the player doesn't have enough inv space
                        if(player.getInventory().firstEmpty() == -1){
                            World playerWorld = player.getWorld();
                            double playerX = player.getLocation().getX();
                            double playerY = player.getLocation().getY();
                            double playerZ = player.getLocation().getZ();
                            Location dropLocation = new Location(playerWorld, playerX+1, playerY, playerZ); //Drop them in front of him

                            playerWorld.dropItem(dropLocation, item);
                        }
                        else player.getInventory().addItem(item);
                    }
                }
            }

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
