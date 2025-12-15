package me.andrew.DiscordUtils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class DiscordBlock implements Listener {
    DiscordUtils plugin;

    private DiscordBlock(DiscordUtils plugin){
        this.plugin = plugin;
    }

    public void showDiscordBlock(){
        //Check if the block is toggled
        boolean toggleBlock = plugin.getConfig().getBoolean("toggle-discord-block");
        if(!toggleBlock) return;

        //Get the block coordinates and checks them
        double blockX, blockY, blockZ;
        try{
            String StringBlockX = plugin.getConfig().getString("block-x");
            String StringBlockY = plugin.getConfig().getString("block-y");
            String StringBlockZ = plugin.getConfig().getString("block-z");

            //Check if one of the coordinates is null
            if(StringBlockX == null || StringBlockY == null || StringBlockZ == null){
                Bukkit.getLogger().warning("[DISCORDUTILS] One of the coordinates of the discord-block is NULL! Block will not show up.");
                return;
            }

            blockX = Double.parseDouble(StringBlockX);
            blockY = Double.parseDouble(StringBlockY);
            blockZ = Double.parseDouble(StringBlockZ);
        } catch (Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] One of the coordinates of the discord-block is INVALID! Block will not show up.");
            return;
        }

        //Get and check the world
        World world = Bukkit.getWorld(plugin.getConfig().getString("block-world"));
        if(world == null){
            Bukkit.getLogger().warning("[DISCORDUTILS] World for discord-block NOT found! Block will not show up.");
            return;
        }

        //Checking the head texture and the facing
        String headTexture = plugin.getConfig().getString("custom-head");
        String blockFacing = plugin.getConfig().getString("facing");
        if(headTexture == null){
;           Bukkit.getLogger().warning("[DISCORDUTILS] The head texture of the discord-block is NULL!");
            return;
        }
        if(blockFacing == null){
            Bukkit.getLogger().warning("[DISCORDUTILS] The block facing of the discord-block is NULL!");
            return;
        }

        //Setting the block
        Location blockLocation = new Location(world, blockX, blockY, blockZ);
        Block discordBlock = blockLocation.getBlock();
        discordBlock.setType(Material.PLAYER_HEAD);

        //Setting the custom block from config
        if(!(discordBlock.getState() instanceof Skull discordSkull)) return;
        discordSkull.setRotation(BlockFace.valueOf(blockFacing));

        PlayerProfile discordBlockProfile =Bukkit.createProfile(UUID.randomUUID());
        discordBlockProfile.setProperty(new ProfileProperty("textures", headTexture));
        discordSkull.setPlayerProfile(discordBlockProfile);
        discordSkull.update(true, false);
    }

    @EventHandler
    public void blockClickEvent(PlayerInteractEvent e){
        FileConfiguration config = plugin.getConfig();
        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();
        Location clickedBlockLocation = e.getClickedBlock().getLocation();

        if(clickedBlock == null) return;
        if(!clickedBlock.getType().equals(Material.PLAYER_HEAD)) return;

        //Checks if the player has permission
        if(!player.hasPermission("discordutils.use")){
            Sound noPermission = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-no-permission-sound")));
            float npsVolume = plugin.getConfig().getInt("pnps-volume");
            float npsPitch = plugin.getConfig().getInt("pnps-pitch");

            String noPermissionMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("block-no-permission-message"));
            player.sendMessage(noPermissionMessage);
            player.playSound(player.getLocation(), noPermission, npsVolume, npsPitch);
            return;
        }


    }
}
