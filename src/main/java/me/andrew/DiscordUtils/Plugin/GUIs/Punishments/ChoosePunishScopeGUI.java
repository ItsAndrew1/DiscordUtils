//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChoosePunishScopeGUI implements Listener {
    private final DiscordUtils plugin;
    private long durationFromWarnings;

    public ChoosePunishScopeGUI(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void showGui(Player player){
        int invSize = 54;
        String title = "Choose Punishment Scope";
        Inventory gui = Bukkit.createInventory(player, invSize, title);

        //Return button
        ItemStack returnButton = createButton(Material.SPECTRAL_ARROW, ChatColor.translateAlternateColorCodes('&', "&c&lRETURN"), null, null);
        gui.setItem(40, returnButton);

        //Discord Scope button
        List<String> discordButtonLore = new ArrayList<>();
        discordButtonLore.add(" ");
        discordButtonLore.add(ChatColor.translateAlternateColorCodes('&', "&eThe punishment will take place only in the &ldiscord server&e."));
        ItemStack discordButton = createButton(Material.PLAYER_HEAD, ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD SCOPE"), discordButtonLore ,"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19");
        gui.setItem(20,  discordButton);

        //Minecraft Scope button
        List<String> mcButtonLore = new ArrayList<>();
        mcButtonLore.add(" ");
        mcButtonLore.add(ChatColor.translateAlternateColorCodes('&', "&eThe punishment will take place only in the &lminecraft server&e."));
        ItemStack minecraftButton = createButton(Material.PLAYER_HEAD, ChatColor.translateAlternateColorCodes('&', "&2&LMINECRAFT SCOPE"), mcButtonLore,"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjFiNjM1YmM5MmRkMjAwMTFkNmU3ZDAxMjU3ZTZlYTczN2I0OWZiN2RlOTQxZDNiYmQxODc3MTA0ZmM1ZWIxZSJ9fX0=");
        gui.setItem(22, minecraftButton);

        //Global Scope button
        List<String> globalButtonLore = new ArrayList<>();
        globalButtonLore.add(" ");
        globalButtonLore.add(ChatColor.translateAlternateColorCodes('&', "&eThe punishment will take place in both &lminecraft &eand &ldiscord server&e."));
        ItemStack globalButton = createButton(Material.PLAYER_HEAD, ChatColor.translateAlternateColorCodes('&', "&6&lGLOBAL SCOPE"), globalButtonLore, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTU3N2M0ZGUxZjUxYTcwNzIyMDIzZTg1NmI1NDNjZDU3MGYxZDBlZTZiOWQxNjdiNTkwMjhjZTFiYzkyZTQ1OCJ9fX0=");
        gui.setItem(24, globalButton);

        player.openInventory(gui);
    }

    public String getStringScope(Player staff) {
        AddingState state = plugin.getPunishmentsAddingStates().get(staff.getUniqueId());

        return switch(state.scope){
            case PunishmentScopes.DISCORD -> ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD");
            case PunishmentScopes.MINECRAFT -> ChatColor.translateAlternateColorCodes('&', "&a&lMINECRAFT");
            case PunishmentScopes.GLOBAL -> ChatColor.translateAlternateColorCodes('&', "&e&lGLOBAL");
        };
    }
    private ItemStack createButton(Material mat, String displayName, List<String> lore, String customHeadValue){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        //Setting custom head
        if(customHeadValue != null){
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            skullMeta.setDisplayName(displayName);
            if(lore != null) skullMeta.setLore(lore);
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", customHeadValue));
            skullMeta.setPlayerProfile(profile);

            item.setItemMeta(skullMeta);
            return item;
        }

        meta.setDisplayName(displayName);
        if(lore != null) meta.setLore(lore);
        item.setItemMeta(meta);

        item.setItemMeta(meta);
        return item;
    }
    private void error(Player player, PunishmentScopes scope, PunishmentType punishmentType){
        String chatPrefix = plugin.getConfig().getString("chat-prefix");
        OfflinePlayer targetPlayer = plugin.getPlayerHeadsGUIs().getClickedPlayer();
        String message = "";

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        switch(punishmentType){
            case PunishmentType.PERM_BAN:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&l BAN");
                    case PunishmentScopes.GLOBAL -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL BAN");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix + "&cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&l BAN");
                }
                break;
            case PunishmentType.PERM_MUTE:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&l MUTE");
                    case PunishmentScopes.GLOBAL -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL  MUTE");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix + "&cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lMUTE");
                }
                break;
            case PunishmentType.TEMP_BAN:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&lBAN");
                    case PunishmentScopes.GLOBAL -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL BAN");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix + "&cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lBAN");
                }
                break;
            case PunishmentType.TEMP_MUTE:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&lMUTE");
                    case PunishmentScopes.GLOBAL -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL MUTE");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix + "&cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lMUTE");
                }
                break;
            case PunishmentType.PERM_BAN_WARN:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&lBAN");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lBAN");
                    case PunishmentScopes.GLOBAL -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL BAN");
                }
                break;
            case PunishmentType.PERM_MUTE_WARN:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&lMUTE");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lMUTE");
                    case PunishmentScopes.GLOBAL ->message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL MUTE");
                }
                break;
            case PunishmentType.TEMP_BAN_WARN:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&lBAN!");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lBAN");
                    case PunishmentScopes.GLOBAL ->message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL BAN");
                }
                break;
            case PunishmentType.TEMP_MUTE_WARN:
                switch(scope){
                    case PunishmentScopes.DISCORD -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &9&lDISCORD &e&lMUTE!");
                    case PunishmentScopes.MINECRAFT -> message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive MINECRAFT &e&lMUTE");
                    case PunishmentScopes.GLOBAL ->message = ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cPlayer &e"+targetPlayer.getName()+" &calready has an &a&lactive &e&lGLOBAL MUTE");
                }
                break;
        }

        player.sendMessage(message);
    }

    private void checkForWarns(Player staff, OfflinePlayer targetPlayer, AddingState state) throws SQLException {
        PunishmentType type = state.type;
        int currentNrOfWarns = plugin.getDatabaseManager().getNrOfWarns(targetPlayer, type, state.scope);
        int designatedNrWarns = plugin.getConfig().getInt("warns-amount");

        if(currentNrOfWarns == designatedNrWarns - 1 && (type == PunishmentType.TEMP_BAN_WARN || type == PunishmentType.TEMP_MUTE_WARN)){
            staff.closeInventory();
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEnter the duration for the temporary ban. Type &c&lcancel &ato exit this."));
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aExample: &e2d 5h 10m 35s"));
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&ld &f-> &adays | &f&lh &f-> &ahours | &f&lm &f-> &aminutes | &f&ls-> &aseconds"));

            plugin.waitForPlayerInput(staff, input -> {
                if(input.equalsIgnoreCase("cancel")){
                    staff.playSound(staff.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    state.scope = null;
                    showGui(staff);
                    return;
                }
                durationFromWarnings = parseCooldown(input);

                if(durationFromWarnings == 0){
                    staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid duration."));
                    //Reopens the gui after 1/2 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            showGui(staff);
                            state.scope = null;
                        }
                    }.runTaskLater(plugin, 10L);
                } else{
                    state.duration = durationFromWarnings;
                    state.lastInteraction = System.currentTimeMillis();
                    plugin.getFinalPunishmentGUI().showGui(staff);
                }
            });
        } else{
            state.lastInteraction = System.currentTimeMillis();
            plugin.getFinalPunishmentGUI().showGui(staff);
        }
    }
    private long parseCooldown(String cooldown){
        long millis = 0;
        Matcher m = Pattern.compile("(\\d+)([dhms])").matcher(cooldown.toLowerCase());

        while(m.find()){
            int value = Integer.parseInt(m.group(1));
            switch(m.group(2)){
                case "d" -> millis += value*86400000L;
                case "h" -> millis += value*3600000L;
                case "m" -> millis += value*60000L;
                case "s" -> millis += value*1000L;
            }
        }
        return millis;
    }

    private boolean isBotConfigured(){
        String guildId = plugin.botFile().getConfig().getString("guild-id");
        String botToken = plugin.botFile().getConfig().getString("bot-token");

        return guildId != null && botToken != null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) throws SQLException {
        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(!e.getView().getTitle().equalsIgnoreCase("Choose Punishment Scope")) return;
        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null) return;
        Material clickedMaterial = clickedItem.getType();

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;

        AddingState state = plugin.getPunishmentsAddingStates().get(player.getUniqueId());

        //If the player clicks on return button
        if(clickedMaterial.equals(Material.SPECTRAL_ARROW)){
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            if(state.scope != null) state.scope = null; //Resets the saved scope
            state.lastInteraction = System.currentTimeMillis();
            plugin.getChoosePunishTypeGUI().showGui(player);
            return;
        }

        OfflinePlayer targetPlayer =  plugin.getPlayerHeadsGUIs().getClickedPlayer();
        //If the player clicks on discord scope button
        if(clickedMaterial.equals(Material.PLAYER_HEAD) && clickedMeta.getDisplayName().contains(ChatColor.translateAlternateColorCodes('&', "&9&lDISCORD"))){
            //Checking if the bot is configured properly
            if(!isBotConfigured()){
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThe &9&lDiscord Bot &cis not set up properly!"));
                return;
            }

            //Check if the target player is banned on discord
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                error(player, PunishmentScopes.DISCORD, state.type);
                return;
            }

            //Check if the target player is banned globally
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                error(player, PunishmentScopes.GLOBAL, state.type);
                return;
            }

            //Check if the target player is muted on discord
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                error(player, PunishmentScopes.DISCORD, state.type);
                return;
            }

            //Check if the target player is muted globally
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                error(player, PunishmentScopes.GLOBAL, state.type);
                return;
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            state.scope = PunishmentScopes.DISCORD;
        }

        //If the player clicks on minecraft scope button
        if(clickedMaterial.equals(Material.PLAYER_HEAD) && clickedMeta.getDisplayName().contains(ChatColor.translateAlternateColorCodes('&', "&2&lMINECRAFT"))){
            //Check if the target player is banned on minecraft
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                error(player, PunishmentScopes.MINECRAFT, state.type);
                return;
            }

            //Check if the target player is banned globally
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                error(player, PunishmentScopes.GLOBAL, state.type);
                return;
            }

            //Check if the target player is muted on minecraft
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                error(player, PunishmentScopes.MINECRAFT, state.type);
                return;
            }

            //Check if the target player is muted globally
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)){
                error(player, PunishmentScopes.GLOBAL, state.type);
                return;
            }

            player.playSound(player.getLocation(),  Sound.UI_BUTTON_CLICK, 1f, 1f);
            state.scope = PunishmentScopes.MINECRAFT;
        }

        //If the player clicks on global scope button
        String chatPrefix = plugin.getConfig().getString("chat-prefix");
        if(clickedMaterial.equals(Material.PLAYER_HEAD) && clickedMeta.getDisplayName().contains(ChatColor.translateAlternateColorCodes('&', "&6&lGLOBAL"))){
            //Check if the player is banned globally
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)) {
                error(player, PunishmentScopes.GLOBAL, state.type);
                return;
            }

            //Check if the player is banned on discord
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                error(player, PunishmentScopes.DISCORD, state.type);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou may choose the &a&lMINECRAFT &coption instead!"));
                return;
            }

            //Check if the player is banned on minecraft
            if(plugin.getDatabaseManager().isPlayerBanned(targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                error(player, PunishmentScopes.MINECRAFT, state.type);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou may choose the &9&lDISCORD &coption instead!"));
                return;
            }

            //Check if the player is muted globally
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.GLOBAL)) {
                error(player, PunishmentScopes.GLOBAL, state.type);
                return;
            }

            //Check if the target player is muted on discord
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.DISCORD)){
                error(player, PunishmentScopes.DISCORD, state.type);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou may choose the &a&lMINECRAFT &coption instead!"));
                return;
            }

            //Check if the target player is muted on minecraft
            if(plugin.getDatabaseManager().isPlayerMuted(targetPlayer.getUniqueId(), PunishmentScopes.MINECRAFT)){
                error(player, PunishmentScopes.MINECRAFT, state.type);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatPrefix+" &cYou may choose the &9&lDISCORD &coption instead!"));
                return;
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            state.scope = PunishmentScopes.GLOBAL;
        }

        checkForWarns(player, targetPlayer, state);
    }
}
