//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RewardsGUI implements Listener {
    public static final String TITLE = "Verification Rewards";
    private static final int[] REWARD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int INFO_SLOT = 4;
    private static final int TOGGLE_SLOT = 46;
    private static final int EXP_SLOT = 48;
    private static final int SAVE_SLOT = 49;
    private static final int CLEAR_SLOT = 51;
    private static final int BACK_SLOT = 53;

    private final DiscordUtils plugin;
    private final Map<UUID, Draft> drafts = new HashMap<>();

    public RewardsGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGUI(Player player) {
        if (!canEdit(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-no-permission-message", "&cYou don't have permission to run this command.")));
            return;
        }

        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            draft = Draft.from(plugin);
            drafts.put(player.getUniqueId(), draft);
        }
        draft.awaitingExp = false;

        Inventory gui = Bukkit.createInventory(null, 54, TITLE);
        ItemStack glass = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < gui.getSize(); slot++) {
            if (!isRewardSlot(slot)) gui.setItem(slot, glass);
        }

        for (int i = 0; i < REWARD_SLOTS.length; i++) {
            gui.setItem(REWARD_SLOTS[i], draft.items[i] == null ? null : draft.items[i].clone());
        }

        gui.setItem(INFO_SLOT, infoItem(draft));
        gui.setItem(TOGGLE_SLOT, toggleItem(draft));
        gui.setItem(EXP_SLOT, expItem(draft));
        gui.setItem(SAVE_SLOT, named(Material.LIME_CONCRETE, "&a&lSAVE SETTINGS"));
        gui.setItem(CLEAR_SLOT, named(Material.BARRIER, "&c&lCLEAR ITEMS", "&7Removes the reward items from this GUI.", "&7Click save afterwards to keep that."));
        gui.setItem(BACK_SLOT, named(Material.SPECTRAL_ARROW, "&e&lBACK"));
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;

        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (event.getAction().name().equals("COLLECT_TO_CURSOR")) {
            event.setCancelled(true);
            return;
        }

        if (raw >= topSize) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                copyIntoFirstEmpty(player, event.getView().getTopInventory(), event.getCurrentItem());
            }
            return;
        }

        event.setCancelled(true);
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) return;

        if (isRewardSlot(raw)) {
            placeOrRemoveCopy(player, event.getView().getTopInventory(), raw, event.getCursor());
            return;
        }

        Sound click = Sounds.fromKey("ui.button.click");
        switch (raw) {
            case TOGGLE_SLOT -> {
                play(player, click);
                draft.toggle = !draft.toggle;
                event.getView().getTopInventory().setItem(TOGGLE_SLOT, toggleItem(draft));
                event.getView().getTopInventory().setItem(INFO_SLOT, infoItem(draft));
            }
            case EXP_SLOT -> {
                play(player, click);
                capture(event.getView().getTopInventory(), draft);
                draft.awaitingExp = true;
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the XP levels to give (&f0 &aor more). Type &fcancel &ato go back."));
                plugin.waitForPlayerInput(player, input -> onExpInput(player, input));
            }
            case SAVE_SLOT -> {
                play(player, click);
                capture(event.getView().getTopInventory(), draft);
                plugin.getVerificationRewards().save(draft.toggle, draft.exp, draft.storedItems());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("chat-prefix", "") + " &aVerification rewards saved."));
                Sound good = Sounds.fromKey("entity.player.levelup");
                play(player, good, 1.4f);
            }
            case CLEAR_SLOT -> {
                play(player, click);
                draft.overflow.clear();
                for (int slot : REWARD_SLOTS) event.getView().getTopInventory().setItem(slot, null);
                capture(event.getView().getTopInventory(), draft);
            }
            case BACK_SLOT -> {
                play(player, click);
                drafts.remove(player.getUniqueId());
                if (player.hasPermission("discordutils.commands.mainconfig")) {
                    plugin.getMainConfigGUI().showGUI(player);
                } else {
                    player.closeInventory();
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(TITLE)) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null || draft.awaitingExp) return;
        drafts.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        drafts.remove(event.getPlayer().getUniqueId());
    }

    private void onExpInput(Player player, String input) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            draft = Draft.from(plugin);
            drafts.put(player.getUniqueId(), draft);
        }

        if (input.equalsIgnoreCase("cancel")) {
            showGUI(player);
            return;
        }

        int levels;
        try {
            levels = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThat is not a whole number. Type the XP levels, or &fcancel&c."));
            plugin.waitForPlayerInput(player, next -> onExpInput(player, next));
            return;
        }

        if (levels < 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cXP levels cannot be negative."));
            plugin.waitForPlayerInput(player, next -> onExpInput(player, next));
            return;
        }

        draft.exp = levels;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aXP reward set to &f" + levels + " &alevels. Click save to keep it."));
        showGUI(player);
    }

    private void placeOrRemoveCopy(Player player, Inventory gui, int slot, ItemStack cursor) {
        if (cursor != null && cursor.getType() != Material.AIR) {
            gui.setItem(slot, cursor.clone());
            play(player, Sounds.fromKey("ui.button.click"));
            return;
        }

        ItemStack current = gui.getItem(slot);
        if (current != null && current.getType() != Material.AIR) {
            gui.setItem(slot, null);
            play(player, Sounds.fromKey("ui.button.click"));
        }
    }

    private void copyIntoFirstEmpty(Player player, Inventory gui, ItemStack source) {
        if (source == null || source.getType() == Material.AIR) return;
        for (int slot : REWARD_SLOTS) {
            ItemStack current = gui.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                gui.setItem(slot, source.clone());
                play(player, Sounds.fromKey("ui.button.click"));
                return;
            }
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("chat-prefix", "") + " &cThere are no empty reward slots."));
    }

    private void capture(Inventory gui, Draft draft) {
        for (int i = 0; i < REWARD_SLOTS.length; i++) {
            ItemStack item = gui.getItem(REWARD_SLOTS[i]);
            draft.items[i] = (item == null || item.getType() == Material.AIR) ? null : item.clone();
        }
    }

    private ItemStack infoItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Shift-click an item, or pick it up and");
        lore.add("&7click a slot, to add a &fcopy&7.");
        lore.add("&7Click a filled slot to remove it.");
        lore.add(" ");
        lore.add("&7Giving rewards: " + (draft.toggle ? "&aON" : "&cOFF"));
        lore.add("&7XP levels: &f" + draft.exp);
        if (!draft.overflow.isEmpty()) {
            lore.add("&7Extra saved items: &f" + draft.overflow.size());
        }
        return named(Material.OAK_SIGN, "&9&lVerification rewards", lore.toArray(String[]::new));
    }

    private ItemStack toggleItem(Draft draft) {
        if (draft.toggle) {
            return named(Material.LIME_DYE, "&a&lREWARDS ENABLED", "&7Players receive these rewards", "&7when they verify.", " ", "&eClick to disable");
        }
        return named(Material.GRAY_DYE, "&c&lREWARDS DISABLED", "&7Players will not receive rewards", "&7when they verify.", " ", "&eClick to enable");
    }

    private ItemStack expItem(Draft draft) {
        return named(Material.EXPERIENCE_BOTTLE, "&b&lSET XP LEVELS", "&7Current: &f" + draft.exp + " levels", " ", "&eClick to type a new amount");
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore.length > 0) {
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(colored);
        }
        item.setItemMeta(meta);
        return item;
    }

    private boolean isRewardSlot(int slot) {
        for (int rewardSlot : REWARD_SLOTS) {
            if (rewardSlot == slot) return true;
        }
        return false;
    }

    private boolean canEdit(Player player) {
        return player.hasPermission("discordutils.commands.rewards") || player.hasPermission("discordutils.commands.mainconfig");
    }

    private void play(Player player, Sound sound) {
        play(player, sound, 1f);
    }

    private void play(Player player, Sound sound, float pitch) {
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1f, pitch);
    }

    private static final class Draft {
        private boolean toggle;
        private int exp;
        private final ItemStack[] items = new ItemStack[REWARD_SLOTS.length];
        private final List<ItemStack> overflow = new ArrayList<>();
        private boolean awaitingExp;

        private static Draft from(DiscordUtils plugin) {
            Draft draft = new Draft();
            draft.toggle = plugin.getVerificationRewards().isEnabled();
            draft.exp = plugin.getVerificationRewards().getExpLevels();
            List<ItemStack> saved = plugin.getVerificationRewards().getItems();
            int visible = Math.min(saved.size(), REWARD_SLOTS.length);
            for (int i = 0; i < visible; i++) draft.items[i] = saved.get(i).clone();
            if (saved.size() > REWARD_SLOTS.length) {
                draft.overflow.addAll(saved.subList(REWARD_SLOTS.length, saved.size()));
            }
            return draft;
        }

        private List<ItemStack> storedItems() {
            List<ItemStack> stored = new ArrayList<>();
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) stored.add(item.clone());
            }
            for (ItemStack item : overflow) {
                if (item != null && item.getType() != Material.AIR) stored.add(item.clone());
            }
            return stored;
        }
    }
}
