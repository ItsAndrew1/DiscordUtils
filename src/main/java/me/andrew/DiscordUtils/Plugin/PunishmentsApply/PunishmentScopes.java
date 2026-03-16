//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public enum PunishmentScopes{
    DISCORD{
        @Override
        public void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException {
            type.apply(ctx);
        }

        @Override
        public boolean playerAlreadyHasPunishment(PunishmentType type, OfflinePlayer offlinePlayer, Player staff, DiscordUtils plugin) throws SQLException{
            if(plugin.getDatabaseManager().playerHasPunishment(offlinePlayer.getUniqueId(), type, DISCORD)){
                staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                staff.sendMessage(ChatColor.RED + "&e"+offlinePlayer.getName()+" &calready has a &9&lDISCORD&c/&a&lMINECRAFT &cpunishment of this type!");
                return true;
            }

            return false;
        }
    },
    MINECRAFT{
        @Override
        public void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException {
            type.apply(ctx);
        }

        @Override
        public boolean playerAlreadyHasPunishment(PunishmentType type, OfflinePlayer offlinePlayer, Player staff, DiscordUtils plugin) throws SQLException{
            if(plugin.getDatabaseManager().playerHasPunishment(offlinePlayer.getUniqueId(), type, MINECRAFT)){
                staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                staff.sendMessage(ChatColor.RED + "&e"+offlinePlayer.getName()+" &calready has a &9&lDISCORD&c/&a&lMINECRAFT &cpunishment of this type!");
                return true;
            }

            return false;
        }
    },
    GLOBAL{
        @Override
        public void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException {
            type.apply(ctx);
        }

        @Override
        public boolean playerAlreadyHasPunishment(PunishmentType type, OfflinePlayer offlinePlayer, Player staff, DiscordUtils plugin) throws SQLException{
            if(plugin.getDatabaseManager().playerHasPunishment(offlinePlayer.getUniqueId(), type, DISCORD) || plugin.getDatabaseManager().playerHasPunishment(offlinePlayer.getUniqueId(), type, MINECRAFT)){
                staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                staff.sendMessage(ChatColor.RED + "&e"+offlinePlayer.getName()+" &calready has a &9&lDISCORD&c/&a&lMINECRAFT &cpunishment of this type!");
                return true;
            }

            if(plugin.getDatabaseManager().playerHasPunishment(offlinePlayer.getUniqueId(), type, GLOBAL)){
                staff.playSound(staff.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                staff.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e"+offlinePlayer.getName()+" &calready has a &e&lGLOBAL &cpunishment of this type!"));
                return true;
            }

            return false;
        }
    };

    //Abstract classes for each type of punishment
    public abstract void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException;
    public abstract boolean playerAlreadyHasPunishment(PunishmentType type, OfflinePlayer offlinePlayer, Player staff, DiscordUtils plugin) throws SQLException;
}
