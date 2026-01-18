//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import java.sql.SQLException;

public enum PunishmentScopes{
    DISCORD{
        @Override
        public void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException {
            type.apply(ctx);
        }
    },
    MINECRAFT{
        @Override
        public void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException {
            type.apply(ctx);
        }
    },
    GLOBAL{
        @Override
        public void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException {
            type.apply(ctx);
        }
    };

    //Abstract classes for each type of punishment
    public abstract void applyPunishment(PunishmentContext ctx, PunishmentType type) throws SQLException;
}
