//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.Punishment;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PunishmentsGUI implements Listener{
    private final DiscordUtils plugin;
    private Connection databaseConnection;
    private final Map<UUID, PunishmentsFilter> filters = new HashMap<>();

    public PunishmentsGUI(DiscordUtils plugin){
        this.plugin = plugin;
    }

    public void showGui(Player player, int page) throws SQLException {
        databaseConnection = plugin.getDatabaseManager().getConnection();
        int invSize = 54;

        //Getting the filtering that the player has
        PunishmentsFilter filter = filters.getOrDefault(player.getUniqueId(), PunishmentsFilter.ALL); //Shows all by default

        String clickPlayerName = plugin.getPlayerHeadsGUIs().getClickedPlayer().getName();
        UUID clickedPlayerUUID = plugin.getPlayerHeadsGUIs().getClickedPlayer().getUniqueId();
        String guiTitle = clickPlayerName+"'s History (Page "+page+")";

        Inventory gui = Bukkit.createInventory(null, invSize, guiTitle);

        //Setting the selected player's head on the top of the GUI
        ItemStack selectedPlayersHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sphMeta =  (SkullMeta) selectedPlayersHead.getItemMeta();
        sphMeta.setOwningPlayer(plugin.getPlayerHeadsGUIs().getClickedPlayer());
        sphMeta.setDisplayName(ChatColor.YELLOW + clickPlayerName);
        selectedPlayersHead.setItemMeta(sphMeta);
        gui.setItem(4, selectedPlayersHead);

        //Decorations
        ItemStack decoGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decoGlassMeta = decoGlass.getItemMeta();
        decoGlassMeta.setDisplayName(" ");
        decoGlass.setItemMeta(decoGlassMeta);
        for(int i = 9; i<=17; i++) gui.setItem(i, decoGlass);

        //Return button
        ItemStack returnButton = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta returnButtonMeta = returnButton.getItemMeta();
        returnButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"));
        returnButton.setItemMeta(returnButtonMeta);
        gui.setItem(49, returnButton);

        //Setting the punishments
        if(!plugin.getDatabaseManager().playerHasPunishments(clickedPlayerUUID)){
            //No punishments item
            ItemStack noPunish = new ItemStack(Material.BARRIER);
            ItemMeta noPunishMeta = noPunish.getItemMeta();
            noPunishMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cPlayer &e"+clickPlayerName+" &chas no punishments yet!"));
            noPunish.setItemMeta(noPunishMeta);
            gui.setItem(31, noPunish);
        }
        else{
            //Filtering buttons
            int nrPlayerActivePunishments = plugin.getDatabaseManager().getPlayerActivePunishmentsNr(clickedPlayerUUID);
            int nrPlayerExpiredPunishments = plugin.getDatabaseManager().getPlayerExpiredPunishmentsNr(clickedPlayerUUID);
            if(nrPlayerActivePunishments >= 1 && nrPlayerExpiredPunishments >= 1){
                if(filter.equals(PunishmentsFilter.ALL)){
                    ItemStack filterAllButton =  new ItemStack(Material.YELLOW_DYE);
                    ItemMeta filterAllButtonMeta = filterAllButton.getItemMeta();

                    filterAllButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lFiltering All Punishments"));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&8Click to change!"));
                    lore.add(" ");
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&a▶  &e&lALL PUNISHMENTS"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "    &a&lACTIVE PUNISHMENTS"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "    &c&lEXPIRED PUNISHMENTS"));
                    lore.add(" ");

                    filterAllButtonMeta.setLore(lore);
                    filterAllButton.setItemMeta(filterAllButtonMeta);
                    gui.setItem(53,  filterAllButton);
                }
                else if(filter.equals(PunishmentsFilter.ACTIVE)){
                    ItemStack filterActiveButton =  new ItemStack(Material.GREEN_DYE);
                    ItemMeta filterActiveButtonMeta = filterActiveButton.getItemMeta();

                    filterActiveButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lFiltering Active Punishments"));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&8Click to change!"));
                    lore.add(" ");
                    lore.add(ChatColor.translateAlternateColorCodes('&', "    &e&lALL PUNISHMENTS"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&a▶  &a&lACTIVE PUNISHMENTS"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "    &c&lEXPIRED PUNISHMENTS"));
                    lore.add(" ");

                    filterActiveButtonMeta.setLore(lore);
                    filterActiveButton.setItemMeta(filterActiveButtonMeta);
                    gui.setItem(53, filterActiveButton);
                }
                else if(filter.equals(PunishmentsFilter.EXPIRED)){
                    ItemStack filterExpiredButton =  new ItemStack(Material.RED_DYE);
                    ItemMeta filterExpiredButtonMeta = filterExpiredButton.getItemMeta();

                    filterExpiredButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lFiltering Expired Punishments"));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&8Click to change!"));
                    lore.add(" ");
                    lore.add(ChatColor.translateAlternateColorCodes('&', "    &e&lALL PUNISHMENTS"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "    &a&lACTIVE PUNISHMENTS"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&a▶  &c&lEXPIRED PUNISHMENTS"));
                    lore.add(" ");

                    filterExpiredButtonMeta.setLore(lore);
                    filterExpiredButton.setItemMeta(filterExpiredButtonMeta);
                    gui.setItem(53, filterExpiredButton);
                }
            }

            int punishmentsPerPage = 27;
            int offset = (page - 1) *  punishmentsPerPage;
            List<Punishment> playerPunishments = plugin.getDatabaseManager().getPlayerPunishments(clickedPlayerUUID, filter, punishmentsPerPage, offset);
            int endIndex = Math.min(offset + punishmentsPerPage, playerPunishments.size());

            //Displaying the punishments
            int startSlot = 18;
            for(int i = 0; i < playerPunishments.size(); i++) gui.setItem(startSlot + i, createPunishmentItem(playerPunishments.get(i)));

            //Setting the page navigation buttons
            if(page > 1){
                ItemStack previousPageButton = new ItemStack(Material.ARROW);
                ItemMeta previousPageButtonItemMeta = previousPageButton.getItemMeta();
                previousPageButtonItemMeta.setDisplayName(ChatColor.RED + "◀ Previous Page");
                gui.setItem(48, previousPageButton);
            }
            if(endIndex < playerPunishments.size()){
                ItemStack nextPageButton = new ItemStack(Material.ARROW);
                ItemMeta nextPageButtonItemMeta = nextPageButton.getItemMeta();
                nextPageButtonItemMeta.setDisplayName(ChatColor.GREEN + "Next Page ▶");
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
        //Setting the material based on the punishment type
        Material pmMat = switch(p.getPunishmentType()){
            case PunishmentType.PERM_BAN, PERM_BAN_WARN -> Material.NETHERITE_AXE;
            case PunishmentType.TEMP_BAN, TEMP_BAN_WARN -> Material.IRON_AXE;
            case PunishmentType.KICK -> Material.LEATHER_BOOTS;
            case PunishmentType.PERM_MUTE, PERM_MUTE_WARN -> Material.SOUL_LANTERN;
            case PunishmentType.TEMP_MUTE, TEMP_MUTE_WARN -> Material.LANTERN;
        };

        ItemStack punishmentItem = new ItemStack(pmMat);
        ItemMeta punishmentItemMeta = punishmentItem.getItemMeta();

        //Setting the display name based on the punishment type
        String displayName = switch(p.getPunishmentType()){
            case PunishmentType.PERM_BAN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT BAN");
            case PunishmentType.PERM_BAN_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT BAN WARN");
            case PunishmentType.TEMP_BAN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY BAN");
            case PunishmentType.TEMP_BAN_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY BAN WARN");
            case PunishmentType.KICK -> ChatColor.translateAlternateColorCodes('&', "&e&lKICK");
            case PunishmentType.PERM_MUTE -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT MUTE");
            case PunishmentType.PERM_MUTE_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lPERMANENT MUTE WARN");
            case PunishmentType.TEMP_MUTE -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY MUTE");
            case PunishmentType.TEMP_MUTE_WARN -> ChatColor.translateAlternateColorCodes('&', "&e&lTEMPORARY MUTE WARN");
        };
        punishmentItemMeta.setDisplayName(displayName);

        //Formatting the time when the punishment was created
        long createdAt = p.getIssuedAt();
        Instant createdInstant = Instant.ofEpochMilli(createdAt);
        LocalDateTime time = LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String formatedTime = time.format(formatter);

        //Formatting the time when the punishment is set to expired
        long expiresAt = p.getExpiresAt();
        Instant expireInstant = Instant.ofEpochMilli(expiresAt);
        LocalDateTime time2 = LocalDateTime.ofInstant(expireInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String expireFormatedTime = time2.format(formatter2);

        //Formatting the time when the punishment was removed
        long removedAt = p.getRemovedAt();
        Instant removedInstant = Instant.ofEpochMilli(removedAt);
        LocalDateTime time3 = LocalDateTime.ofInstant(removedInstant, ZoneId.systemDefault());
        DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String removedFormatedTime = time3.format(formatter3);

        //Formats the scope of the punishment
        String punishmentScope = switch(p.getScope()){
            case PunishmentScopes.MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case PunishmentScopes.DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case PunishmentScopes.GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
        };

        //Adds enchant glint if the punishment is a warning
        if(p.getPunishmentType().toString().contains("WARN")){
            punishmentItemMeta.addEnchant(Enchantment.LURE, 1, true);
            punishmentItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        //Setting the lore
        List<String> itemLore = new ArrayList<>();
        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&8Issued at: &l"+formatedTime));
        itemLore.add(" ");
        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&aScope: "+punishmentScope));
        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&aStaff: &e&l"+p.getStaff()));
        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&aReason: &e"+p.getReason()));
        itemLore.add(" ");

        //Setting the status
        String status;
        if(p.isRemoved()) status = ChatColor.translateAlternateColorCodes('&', "&aStatus: &4&lREMOVED");
        else if(p.isActive()) status = ChatColor.translateAlternateColorCodes('&', "&aStatus: &lACTIVE");
        else status = ChatColor.translateAlternateColorCodes('&', "&aStatus: &c&lEXPIRED");
        itemLore.add(status);

        if(p.isRemoved()) itemLore.add(ChatColor.translateAlternateColorCodes('&', "&8Removed at &l"+removedFormatedTime));
        if(p.isActive() && !p.getPunishmentType().toString().contains("WARN")){
            itemLore.add(ChatColor.translateAlternateColorCodes('&', "&aID: &8"+p.getId()));
            if(!isPermanent(p)) itemLore.add(ChatColor.translateAlternateColorCodes('&', "&8Expires at &l"+expireFormatedTime));
        }

        punishmentItemMeta.setLore(itemLore);
        punishmentItem.setItemMeta(punishmentItemMeta);
        return punishmentItem;
    }

    private boolean isPermanent(Punishment p){
        return p.getExpiresAt() == 0;
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
            plugin.getAddRemovePunishGUI().showGui(player);
            return;
        }

        //If the player clicks on next page
        if(clickedMeta.getDisplayName().contains(ChatColor.translateAlternateColorCodes('&', "&caNext Page"))){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            showGui(player, getPageNrFromTitle(event.getView().getTitle()) + 1);
            return;
        }

        //If the player clicks on previous page
        if(clickedMeta.getDisplayName().contains(ChatColor.translateAlternateColorCodes('&', "&cPrevious Page"))){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            showGui(player, getPageNrFromTitle(event.getView().getTitle()) - 1);
            return;
        }

        //If the player clicks on the filtering buttons
        if(clickedMaterial.equals(Material.YELLOW_DYE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            filters.put(player.getUniqueId(), PunishmentsFilter.ACTIVE);
            showGui(player, 1);
        }
        if(clickedMaterial.equals(Material.GREEN_DYE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            filters.put(player.getUniqueId(), PunishmentsFilter.EXPIRED);
            showGui(player, 1);
        }
        if(clickedMaterial.equals(Material.RED_DYE)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            filters.put(player.getUniqueId(), PunishmentsFilter.ALL);
            showGui(player, 1);
        }
    }
}
