//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Reads, saves and gives the items / XP levels a player gets after a successful verification.
public class VerificationRewards {
    private final DiscordUtils plugin;

    public VerificationRewards(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("rewards.toggle-giving-rewards", false);
    }

    public int getExpLevels() {
        return Math.max(0, plugin.getConfig().getInt("rewards.exp", 0));
    }

    public List<ItemStack> getItems() {
        return readItems(plugin.getConfig());
    }

    public void save(boolean enabled, int expLevels, List<ItemStack> items) {
        FileConfiguration config = plugin.getConfig();
        List<ItemStack> stored = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) continue;
                stored.add(item.clone());
            }
        }

        config.set("rewards.toggle-giving-rewards", enabled);
        config.set("rewards.exp", Math.max(0, expLevels));
        if (!config.contains("rewards.received-message")) {
            config.set("rewards.received-message", "&aYou received your verification rewards!");
        }
        config.set("rewards.items", stored);
        plugin.saveConfig();
    }

    public void give(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!isEnabled()) return;

        int levels = getExpLevels();
        List<ItemStack> items;
        try {
            items = getItems();
        } catch (Exception e) {
            plugin.getLogger().warning("[DISCORDUTILS] Could not read verification rewards: " + e.getMessage());
            sendError(player);
            return;
        }

        if (levels == 0 && items.isEmpty()) return;

        try {
            if (levels > 0) player.giveExpLevels(levels);
            for (ItemStack item : items) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }

            String received = plugin.getConfig().getString("rewards.received-message", "&aYou received your verification rewards!");
            if (!received.isBlank()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', received));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[DISCORDUTILS] Could not give verification rewards to " + player.getName() + ": " + e.getMessage());
            sendError(player);
        }
    }

    private void sendError(Player player) {
        String message = plugin.getConfig().getString("error-giving-rewards-message", "&cThere was a problem giving you the rewards. Contact the server administrators about this.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> readItems(FileConfiguration config) {
        List<ItemStack> items = new ArrayList<>();
        List<?> raw = config.getList("rewards.items");
        if (raw == null) return items;

        for (Object entry : raw) {
            ItemStack stack = null;
            if (entry instanceof ItemStack item) {
                stack = item;
            } else if (entry instanceof Map<?, ?> map) {
                try {
                    stack = ItemStack.deserialize((Map<String, Object>) map);
                } catch (Exception e) {
                    plugin.getLogger().warning("[DISCORDUTILS] Skipped an invalid reward item: " + e.getMessage());
                }
            }

            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;
            items.add(stack.clone());
        }
        return items;
    }
}
