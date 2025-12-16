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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DiscordBlock implements Listener {
    private final DiscordUtils plugin;
    private BukkitTask particleTask;
    private HashMap<Player, BukkitRunnable> playerTasks =  new HashMap<>();

    public DiscordBlock(DiscordUtils plugin){
        this.plugin = plugin;
    }

    public void spawnDiscordBlock(){
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
        discordBlockProfile.setProperty(new ProfileProperty("textures", headTexture));
        discordSkull.setPlayerProfile(discordBlockProfile);
        discordSkull.update(true, false);
    }

    public void startParticleTask(){
        FileConfiguration config = plugin.getConfig();

        //Checks if the particles are toggled
        boolean toggleParticle = config.getBoolean("toggle-discord-block-particles");
        if(!toggleParticle) return;

        //Gets the particle
        Particle blockParticle;
        String particleValue = config.getString("discord-block-particle");
        try{
            blockParticle = getParticle(particleValue);
        } catch (Exception e){
            Bukkit.getLogger().warning("[DISCORDUTILS] The value for discord-block-particle is invalid!");
            return;
        }

        //Getting the necessary data from the config to spawn the particles
        double blockX, blockY, blockZ;
        try{
            String StringBlockX = plugin.getConfig().getString("block-x");
            String StringBlockY = plugin.getConfig().getString("block-y");
            String StringBlockZ = plugin.getConfig().getString("block-z");

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
        World world = Bukkit.getWorld(plugin.getConfig().getString("block-world"));
        if(world == null){
            Bukkit.getLogger().warning("[DISCORDUTILS] World for discord-block NOT found! Particle will not show up.");
            return;
        }

        double offsetX = config.getDouble("offsetX");
        double offsetY = config.getDouble("offsetY");
        double offsetZ = config.getDouble("offsetZ");
        double extra = config.getDouble("extra");
        int particleCount = config.getInt("particle-count");
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
        FileConfiguration config = plugin.getConfig();
        if(e.getClickedBlock() == null) return;
        if(!e.getClickedBlock().getType().equals(Material.PLAYER_HEAD)) return;

        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();
        Location clickedBlockLocation = clickedBlock.getLocation();

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

        //Get the location of the discord-block, then check if it matches with the clicked block
        double blockX, blockY, blockZ;
        World world;
        blockX = config.getDouble("block-x");
        blockY = config.getDouble("block-y");
        blockZ = config.getDouble("block-z");
        world = Bukkit.getWorld(plugin.getConfig().getString("block-world"));
        Location discordBlockLocation = new Location(world, blockX, blockY, blockZ);

        if(clickedBlockLocation.equals(discordBlockLocation)){
            e.setCancelled(true); //Makes the block unbreakable

            //If the fetching data task is toggled and running (BUG HERE -> Double Tasks)
            boolean toggleFetchingData = config.getBoolean("fetching-data");
            if(toggleFetchingData) {
                BukkitRunnable fetchingDataTask = plugin.getDiscordTaskManager().getFetchingDataTask();
                if (fetchingDataTask != null) { //If there is any task running
                    if (playerTasks.containsKey(player)) {
                        Sound taskInProgress = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("fetching-data-task-in-progress").toLowerCase()));
                        float volume = plugin.getConfig().getInt("fdtips-volume");
                        float pitch = plugin.getConfig().getInt("fdtips-pitch");
                        player.playSound(player.getLocation(), taskInProgress, volume, pitch);

                        //Replaces the %% placeholder
                        int taskInterval = plugin.getConfig().getInt("fetching-data.interval");
                        String chatMessage = plugin.getConfig().getString("fetching-data-task-in-progress-message").replace("%task_interval%", String.valueOf(taskInterval));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessage));
                        return;
                    }
                }
                playerTasks.put(player, fetchingDataTask);
            }
            plugin.getDiscordTaskManager().handleTask(player); //Do giveDiscordLink() instead of handleTask() I think
        }
    }

    public HashMap<Player, BukkitRunnable> getPlayerTasks(){
        return playerTasks;
    }
}

