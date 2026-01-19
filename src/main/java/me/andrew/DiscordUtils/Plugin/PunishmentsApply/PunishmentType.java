//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import java.sql.SQLException;

public enum PunishmentType{
    PERM_BAN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermBan();
        }
    },

    TEMP_BAN(false){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyTempBan();
        }
    },

    PERM_BAN_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermBanWarn();
        }
    },

    TEMP_BAN_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyTempBanWarn();
        }
    },

    KICK(false){
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyKick();
        }
    },

    PERM_MUTE(true){
        @Override
        public void apply(PunishmentContext ctx) throws SQLException {
            ctx.applyPermMuteTimeout();
        }
    },

    TEMP_MUTE(false){
        @Override
        public void apply(PunishmentContext ctx) {

        }
    },
    PERM_MUTE_WARN(true){
        @Override
        public void apply(PunishmentContext ctx){

        }
    },

    TEMP_MUTE_WARN(true){
        @Override
        public void apply(PunishmentContext ctx) {

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
}
