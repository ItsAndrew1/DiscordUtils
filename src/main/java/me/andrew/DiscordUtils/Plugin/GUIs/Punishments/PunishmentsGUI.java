//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Caching.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PunishmentsGUI implements Listener{
    private final DiscordUtils plugin;
    private final Map<UUID, PunishmentsFilter> filters = new HashMap<>();
    private final Map<UUID, Boolean> selfMap = new HashMap<>();
    private final NamespacedKey container;

    public PunishmentsGUI(DiscordUtils plugin){
        this.plugin = plugin;
        this.container = new NamespacedKey(plugin, "NextPrevious");
    }

    public void showGui(Player player, int page, boolean self) {
        if(!selfMap.containsKey(player.getUniqueId())) selfMap.put(player.getUniqueId(), self);

        int invSize = 54;
        //Getting the filtering that the player has
        PunishmentsFilter filter = filters.getOrDefault(player.getUniqueId(), PunishmentsFilter.ALL); //Shows all by default

        //Getting the main data
        String clickPlayerName;
        UUID playerUUID;
        Component guiTitle;
        if(selfMap.get(player.getUniqueId())){
            clickPlayerName = player.getName();
            playerUUID = player.getUniqueId();
            guiTitle = Component.text("Your History (Page "+page+")");
        }
        else{
            clickPlayerName = plugin.getPlayerHeadsGUIs().getClickedPlayer().getName();
            playerUUID = plugin.getPlayerHeadsGUIs().getClickedPlayer().getUniqueId();
            guiTitle = Component.text(clickPlayerName+"'s History (Page "+page+")");
        }

        Inventory gui = Bukkit.createInventory(null, invSize, guiTitle);

        //Setting the selected player's head on the top of the GUI
        ItemStack selectedPlayersHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sphMeta = (SkullMeta) selectedPlayersHead.getItemMeta();

        if(selfMap.get(player.getUniqueId())) sphMeta.setOwningPlayer(player);
        else sphMeta.setOwningPlayer(plugin.getPlayerHeadsGUIs().getClickedPlayer());

        sphMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e"+clickPlayerName));
        selectedPlayersHead.setItemMeta(sphMeta);
        gui.setItem(4, selectedPlayersHead);

        //Decorations
        ItemStack decoGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decoGlassMeta = decoGlass.getItemMeta();
        decoGlassMeta.displayName(Component.text(" "));
        decoGlass.setItemMeta(decoGlassMeta);
        for(int i = 9; i<=17; i++) gui.setItem(i, decoGlass);

        //Return/Exit button
        ItemStack returnButton = new ItemStack(selfMap.get(player.getUniqueId()) ? Material.RED_CONCRETE : Material.SPECTRAL_ARROW);
        ItemMeta returnButtonMeta = returnButton.getItemMeta();
        returnButtonMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(selfMap.get(player.getUniqueId()) ? "&c&lEXIT" : "&c&lRETURN"));
        returnButton.setItemMeta(returnButtonMeta);
        gui.setItem(49, returnButton);

        //Setting the punishments
        if(!plugin.getPlayerPunishmentDataCache().playerHasPunishments(playerUUID)){
            //No punishments item
            ItemStack noPunish = new ItemStack(Material.BARRIER);
            ItemMeta noPunishMeta = noPunish.getItemMeta();
            noPunishMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&cPlayer &e"+clickPlayerName+" &chas no punishments yet!"));
            noPunish.setItemMeta(noPunishMeta);
            gui.setItem(31, noPunish);
        }
        else{
            //Filtering buttons
            int nrPlayerActivePunishments = plugin.getPlayerPunishmentDataCache().getNrOfPunishments(playerUUID, true);
            int nrPlayerExpiredPunishments = plugin.getPlayerPunishmentDataCache().getNrOfPunishments(playerUUID, false);
            if(nrPlayerActivePunishments >= 1 && nrPlayerExpiredPunishments >= 1){
                if(filter.equals(PunishmentsFilter.ALL)){
                    ItemStack filterAllButton =  new ItemStack(Material.YELLOW_DYE);
                    ItemMeta filterAllButtonMeta = filterAllButton.getItemMeta();

                    filterAllButtonMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e&lFiltering All Punishments"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Click to change!"));
                    lore.add(Component.text(" "));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a▶  &e&lALL PUNISHMENTS"));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("    &a&lACTIVE PUNISHMENTS"));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("    &c&lEXPIRED PUNISHMENTS"));
                    lore.add(Component.text(" "));

                    filterAllButtonMeta.lore(lore);
                    filterAllButton.setItemMeta(filterAllButtonMeta);
                    gui.setItem(53,  filterAllButton);
                }
                else if(filter.equals(PunishmentsFilter.ACTIVE)){
                    ItemStack filterActiveButton =  new ItemStack(Material.GREEN_DYE);
                    ItemMeta filterActiveButtonMeta = filterActiveButton.getItemMeta();

                    filterActiveButtonMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("&a&lFiltering Active Punishments"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Click to change!"));
                    lore.add(Component.text(" "));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("    &e&lALL PUNISHMENTS"));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a▶  &a&lACTIVE PUNISHMENTS"));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("    &c&lEXPIRED PUNISHMENTS"));
                    lore.add(Component.text(" "));

                    filterActiveButtonMeta.lore(lore);
                    filterActiveButton.setItemMeta(filterActiveButtonMeta);
                    gui.setItem(53, filterActiveButton);
                }
                else if(filter.equals(PunishmentsFilter.EXPIRED)){
                    ItemStack filterExpiredButton =  new ItemStack(Material.RED_DYE);
                    ItemMeta filterExpiredButtonMeta = filterExpiredButton.getItemMeta();

                    filterExpiredButtonMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&lFiltering Expired Punishments"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Click to change!"));
                    lore.add(Component.text(" "));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("    &e&lALL PUNISHMENTS"));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("    &a&lACTIVE PUNISHMENTS"));
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a▶  &c&lEXPIRED PUNISHMENTS"));
                    lore.add(Component.text(" "));

                    filterExpiredButtonMeta.lore(lore);
                    filterExpiredButton.setItemMeta(filterExpiredButtonMeta);
                    gui.setItem(53, filterExpiredButton);
                }
            }

            int limit = 27;
            int offset = (page - 1) *  limit;
            CopyOnWriteArrayList<Punishment> playerPunishments = plugin.getPlayerPunishmentDataCache().getPlayerPunishments(playerUUID, filter, limit, offset);
            int endIndex = Math.min(offset + limit, playerPunishments.size());

            //Displaying the punishments
            int startSlot = 18;
            for(int i = 0; i < playerPunishments.size(); i++) gui.setItem(startSlot + i, createPunishmentItem(playerPunishments.get(i)));

            //Setting the page navigation buttons
            if(page > 1){
                ItemStack previousPageButton = new ItemStack(Material.ARROW);
                ItemMeta previousPageButtonItemMeta = previousPageButton.getItemMeta();
                previousPageButtonItemMeta.getPersistentDataContainer().set(container, PersistentDataType.STRING, "previous");
                previousPageButtonItemMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("&c◀ Previous Page"));
                gui.setItem(48, previousPageButton);
            }
            if(endIndex < playerPunishments.size()){
                ItemStack nextPageButton = new ItemStack(Material.ARROW);
                ItemMeta nextPageButtonItemMeta = nextPageButton.getItemMeta();
                nextPageButtonItemMeta.getPersistentDataContainer().set(container, PersistentDataType.STRING, "next");
                nextPageButtonItemMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&aNext Page ▶"));
                gui.setItem(50, nextPageButton);
            }
        }

        player.openInventory(gui);
    }


    private int getPageNrFromTitle(String title) {
        String parsedTitle = title.replaceAll("[^0-9]", "");
        return Integer.parseInt(parsedTitle)-1;
    }

    private ItemStack createPunishmentItem(Punishment p){
        ItemStack punishmentItem = new ItemStack(p.punishmentType().getMaterial());
        ItemMeta punishmentItemMeta = punishmentItem.getItemMeta();

        punishmentItemMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e&l"+p.punishmentType().getName()));

        //Formatting the time when the punishment was created
        long createdAt = p.createdAt();
        Instant createdInstant = Instant.ofEpochMilli(createdAt);
        LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String formatedTime = time.format(formatter);

        //Formatting the time when the punishment is set to expired
        long expiresAt = p.expiresAt();
        Instant expireInstant = Instant.ofEpochMilli(expiresAt);
        LocalDateTime time2 = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String expireFormatedTime = time2.format(formatter2);

        //Formatting the time when the punishment was removed
        long removedAt = p.removedAt();
        Instant removedInstant = Instant.ofEpochMilli(removedAt);
        LocalDateTime time3 = LocalDateTime.ofInstant(removedInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String removedFormatedTime = time3.format(formatter3);

        //Formats the scope of the punishment
        String punishmentScope = switch(p.scope()){
            case MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
        };

        //Adds enchant glint if the punishment is a warning
        if(p.punishmentType().toString().contains("WARN")){
            punishmentItemMeta.addEnchant(Enchantment.LURE, 1, true);
            punishmentItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        //Setting the lore
        List<Component> itemLore = new ArrayList<>();
        itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Issued at: &l"+formatedTime));
        itemLore.add(Component.text(" "));
        itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aScope: "+punishmentScope));
        itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aStaff: &e&l"+p.staff()));

        String reason = p.reason();
        reason = plugin.parsePP(Bukkit.getPlayer(p.uuid()), reason);
        itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aReason: &e"+reason));
        itemLore.add(Component.text(" "));

        //Setting the status
        Component status;
        if(p.removed()) status = LegacyComponentSerializer.legacyAmpersand().deserialize("&aStatus: &4&lREMOVED");
        else if(p.active()) status = LegacyComponentSerializer.legacyAmpersand().deserialize("&aStatus: &lACTIVE");
        else status = LegacyComponentSerializer.legacyAmpersand().deserialize("&aStatus: &c&lEXPIRED");
        itemLore.add(status);

        if(p.removed()) itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Removed at &l"+removedFormatedTime));
        if(p.active() && !p.punishmentType().toString().contains("WARN")){
            itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&aID: &8"+p.id()));
            if(!isPermanent(p)) itemLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8Expires at &l"+expireFormatedTime));
        }

        punishmentItemMeta.lore(itemLore);
        punishmentItem.setItemMeta(punishmentItemMeta);
        return punishmentItem;
    }

    private boolean isPermanent(Punishment p){
        return p.expiresAt() == 0;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) throws SQLException {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!event.getView().getTitle().contains("History")) return;
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null) return;

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;
        Material clickedMaterial = clickedItem.getType();

        //If the player clicks on return button
        Material returnButton = Material.SPECTRAL_ARROW;
        if(clickedMaterial.equals(returnButton)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            //Removing the player from the selfMap
            selfMap.remove(player.getUniqueId());

            plugin.getAddRemovePunishGUI().showGui(player);
        }

        //If the player clicks on exit button
        Material exitButton = Material.RED_CONCRETE;
        if(clickedMaterial.equals(exitButton)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            //Removing the player from the selfMap
            selfMap.remove(player.getUniqueId());

            player.closeInventory();
        }

        //If the player clicks on the filtering buttons
        if(clickedMaterial.equals(Material.YELLOW_DYE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            filters.put(player.getUniqueId(), PunishmentsFilter.ACTIVE);
            showGui(player, 1, selfMap.get(player.getUniqueId()));
        }
        if(clickedMaterial.equals(Material.GREEN_DYE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            filters.put(player.getUniqueId(), PunishmentsFilter.EXPIRED);
            showGui(player, 1, selfMap.get(player.getUniqueId()));
        }
        if(clickedMaterial.equals(Material.RED_DYE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            filters.put(player.getUniqueId(), PunishmentsFilter.ALL);
            showGui(player, 1, selfMap.get(player.getUniqueId()));
        }

        String clickedID = clickedMeta.getPersistentDataContainer().get(container, PersistentDataType.STRING);
        if(clickedID == null) return;

        //If the player clicks on next page
        if(clickedID.equals("next")){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            showGui(player, getPageNrFromTitle(event.getView().getTitle()) + 1, selfMap.get(player.getUniqueId()));
        }

        //If the player clicks on previous page
        if(clickedID.equals("previous")){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            showGui(player, getPageNrFromTitle(event.getView().getTitle()) - 1, selfMap.get(player.getUniqueId()));
        }
    }
}
