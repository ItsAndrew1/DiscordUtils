//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DiscordBlock implements Listener {
    private final DiscordUtils plugin;
    private BukkitTask particleTask;
    private final Set<UUID> cooldowns = new HashSet<>();

    public DiscordBlock(DiscordUtils plugin){
        this.plugin = plugin;
    }

    public void spawnDiscordBlock(){
        //Get the block coordinates and checks them
        double blockX, blockY, blockZ;
        try{
            String StringBlockX = plugin.getConfig().getString("discord-block.location.x");
            String StringBlockY = plugin.getConfig().getString("discord-block.location.y");
            String StringBlockZ = plugin.getConfig().getString("discord-block.location.z");

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
        World world = Bukkit.getWorld(plugin.getConfig().getString("discord-block.world"));
        if(world == null){
            Bukkit.getLogger().warning("[DISCORDUTILS] World for discord-block NOT found! Block will not show up.");
            return;
        }

        //Checking the head texture and the facing
        String blockFacing = plugin.getConfig().getString("discord-block.facing", "NORTH_EAST");

        //Spawning/despawning the block
        boolean toggleBlock = plugin.getConfig().getBoolean("discord-block.toggle", false);
        Location blockLocation = new Location(world, blockX, blockY, blockZ);
        Block discordBlock = blockLocation.getBlock();
        if(toggleBlock) discordBlock.setType(Material.PLAYER_HEAD);
        else{
            discordBlock.setType(Material.AIR); //Despawns the block
            return;
        }

        //Setting the custom block from config
        if(!(discordBlock.getState() instanceof Skull discordSkull)) return;

        //Get and set the block facing
        BlockFace blockFace;
        try{
            blockFace = getBlockFacing(blockFacing);
            discordSkull.setRotation(blockFace);
        } catch (Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] The FACING of discord-block is INVALID!");
            return;
        }

        PlayerProfile discordBlockProfile =Bukkit.createProfile(UUID.randomUUID());
        //Check if the head texture is valid or not
        try{
            String headTexture = plugin.getConfig().getString("discord-block.custom-head", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19");
            discordBlockProfile.setProperty(new ProfileProperty("textures", headTexture));
            discordSkull.setPlayerProfile(discordBlockProfile);
            discordSkull.update(true, false);
        } catch (Exception e) {
            Bukkit.getLogger().warning("DISCORDUTILS] The texture of the block is INVALID!");
        }
        discordSkull.setPlayerProfile(discordBlockProfile);
        discordSkull.update(true, false);
    }

    public void startParticleTask(){
        FileConfiguration config = plugin.getConfig();

        //Checks if the particles are toggled
        boolean toggleParticle = config.getBoolean("discord-block.particles.toggle", false);
        if(!toggleParticle) return;

        //Gets the particle
        Particle blockParticle;
        String particleValue = config.getString("discord-block.particles.particle", "PORTAL");
        try{
            blockParticle = getParticle(particleValue);
        } catch (Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] The value for discord-block-particle is invalid!");
            return;
        }

        //Getting the necessary data from the config to spawn the particles
        double blockX, blockY, blockZ;
        try{
            String StringBlockX = plugin.getConfig().getString("discord-block.location.x");
            String StringBlockY = plugin.getConfig().getString("discord-block.location.y");
            String StringBlockZ = plugin.getConfig().getString("discord-block.location.z");

            //Check if one of the coordinates is null
            if(StringBlockX == null || StringBlockY == null || StringBlockZ == null){
                Bukkit.getLogger().warning("[DISCORDUTILS] One of the coordinates of the discord-block is NULL! Particle will not show up.");
                return;
            }

            blockX = Double.parseDouble(StringBlockX) + 0.5;
            blockY = Double.parseDouble(StringBlockY);
            blockZ = Double.parseDouble(StringBlockZ) + 0.5;
        } catch (Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] One of the coordinates of the discord-block is INVALID! Particle will not show up.");
            return;
        }

        //Get and check the world
        World world = Bukkit.getWorld(plugin.getConfig().getString("discord-block.world"));
        if(world == null){
            Bukkit.getLogger().warning("[DISCORDUTILS] World for discord-block NOT found! Particle will not show up.");
            return;
        }

        double offsetX = config.getDouble("discord-block.particles.offsetX", 0.4);
        double offsetY = config.getDouble("discord-block.particles.offsetY", 0.5);
        double offsetZ = config.getDouble("discord-block.particles.offsetZ", 0.4);
        double extra = config.getDouble("discord-block.particles.extra", 0);
        int particleCount = config.getInt("discord-block.particles.count", 40);
        Location blockLocation = new Location(world, blockX, blockY, blockZ);

        //Starts the particle task
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, ()->{
            for(Player p : Bukkit.getOnlinePlayers()){
                p.spawnParticle(blockParticle, blockLocation, particleCount, offsetX, offsetY, offsetZ, extra);
            }
        }, 0L, 10L);
    }

    private Particle getParticle(String value){
        return Particle.valueOf(value.toUpperCase());
    }
    private BlockFace getBlockFacing(String value){
        return BlockFace.valueOf(value.toUpperCase());
    }
    public BukkitTask getParticleTask(){
        return particleTask;
    }

    @EventHandler
    public void blockClickEvent(PlayerInteractEvent e){
        if(e.getClickedBlock() == null) return;
        if(e.getHand() != EquipmentSlot.HAND) return;
        if(!e.getClickedBlock().getType().equals(Material.PLAYER_HEAD)) return;

        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();
        Location clickedBlockLocation = clickedBlock.getLocation();

        //Checks if the player has permission
        if(!player.hasPermission("discordutils.discord")){
            Sound noPermission = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("player-no-permission-sound")));
            float npsVolume = plugin.getConfig().getInt("pnps-volume");
            float npsPitch = plugin.getConfig().getInt("pnps-pitch");

            String noPermissionMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("block-no-permission-message"));
            player.sendMessage(noPermissionMessage);
            player.playSound(player.getLocation(), noPermission, npsVolume, npsPitch);
            return;
        }

        //Get the location of the discord-block, then check if it matches with the clicked block
        double blockX, blockY, blockZ;
        World world;
        blockX = plugin.getConfig().getDouble("discord-block.location.x");
        blockY = plugin.getConfig().getDouble("discord-block.location.y");
        blockZ = plugin.getConfig().getDouble("discord-block.location.z");
        world = Bukkit.getWorld(plugin.getConfig().getString("discord-block.world"));
        Location discordBlockLocation = new Location(world, blockX, blockY, blockZ);

        if(clickedBlockLocation.equals(discordBlockLocation)){
            e.setCancelled(true); //Makes the block unbreakable
            //Checking if the fetching data task is toggled
            boolean toggleMiniTask = plugin.getConfig().getBoolean("fetching-data.toggle", false);
            if(!toggleMiniTask) plugin.getDiscordTaskManager().giveDiscordLink(player);
            else{
                //Checks if the player already has the task running
                if(cooldowns.contains(player.getUniqueId())){
                    //Plays the sound from config
                    Sound taskInProgress = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("fetching-data-task-in-progress-sound").toLowerCase()));
                    float fdtipsVolume = plugin.getConfig().getInt("fdtips-volume");
                    float fdtipsPitch = plugin.getConfig().getInt("fdtips-pitch");
                    player.playSound(player.getLocation(), taskInProgress, fdtipsVolume, fdtipsPitch);

                    //Sends the message from config
                    String chatMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("fetching-data-task-in-progress-message"));
                    player.sendMessage(chatMessage);
                    return;
                }

                cooldowns.add(player.getUniqueId());
                int duration = plugin.getConfig().getInt("fetching-data.duration");

                //Sends the fetching data chat message
                String fdChatMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("fetching-data-chat-message"));
                fdChatMessage = plugin.parsePP(player, fdChatMessage);
                player.sendMessage(fdChatMessage);

                new BukkitRunnable(){
                    @Override
                    public void run() {
                        plugin.getDiscordTaskManager().giveDiscordLink(player);
                        cooldowns.remove(player.getUniqueId());
                    }
                }.runTaskLater(plugin, duration*20L);
            }
        }
    }

    public Set<UUID> getCooldowns(){
        return cooldowns;
    }
}