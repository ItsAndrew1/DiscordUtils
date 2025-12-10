package me.andrew.DiscordUtils;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Commands implements CommandExecutor {
    private final DiscordUtils plugin;

    public Commands(DiscordUtils plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) sender;

        Sound good = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.player.levelup"));
        Sound invalid = Registry.SOUNDS.get(NamespacedKey.minecraft("entity.enderman.teleport"));
        if(command.getName().equalsIgnoreCase("discordLink")){
            if(!sender.hasPermission("discordlink.staff")){
                player.playSound(player.getLocation(), invalid, 1f, 1f);
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }


        }

        return false;
    }
}
