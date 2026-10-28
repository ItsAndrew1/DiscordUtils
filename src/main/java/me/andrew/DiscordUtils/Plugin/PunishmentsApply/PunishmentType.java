//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import org.bukkit.entity.Player;

import java.sql.SQLException;

public enum PunishmentType{
    PERM_BAN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermBan();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.permban");
        }

        @Override
        public String getName(){
            return "Permanent Ban";
        }
    },

    TEMP_BAN(false){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyTempBan();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.tempban");
        }

        @Override
        public String getName(){
            return "Temporary Ban";
        }
    },

    PERM_BAN_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermBanWarn();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.permbanwarn");
        }

        @Override
        public String getName(){
            return "Permanent Ban Warning";
        }
    },

    TEMP_BAN_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyTempBanWarn();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.tempbanwarn");
        }

        @Override
        public String getName(){
            return "Temporary Ban Warning";
        }
    },

    KICK(true){
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyKick();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.kick");
        }

        @Override
        public String getName(){
            return "Kick";
        }
    },

    PERM_MUTE(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermMuteTimeout();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.permmute");
        }

        @Override
        public String getName(){
            return "Permanent Mute/Timeout";
        }
    },

    TEMP_MUTE(false){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyTempMuteTimeout();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.tempmute");
        }

        @Override
        public String getName(){
            return "Temporary Mute/Timeout";
        }
    },
    PERM_MUTE_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermMuteTimeoutWarn();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.permmutewarn");
        }

        @Override
        public String getName(){
            return "Permanent Mute/Timeout Warning";
        }
    },

    TEMP_MUTE_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyTempMuteTimeoutWarn();
        }

        @Override
        public boolean hasPermission(Player player){
            return player.hasPermission("discordutils.punishments.add.tempmutewarn");
        }

        @Override
        public String getName(){
            return "Temporary Mute/Timeout Warning";
        }
    };

    private final boolean permanent;
    PunishmentType(boolean permanent){
        this.permanent = permanent;
    }

    //Helper check
    public boolean isPermanent(){
        return permanent;
    }

    public abstract void apply(PunishmentContext ctx) throws SQLException;
    public abstract boolean hasPermission(Player player);
    public abstract String getName();
}
