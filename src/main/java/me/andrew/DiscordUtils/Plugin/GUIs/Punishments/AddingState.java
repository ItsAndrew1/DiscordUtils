package me.andrew.DiscordUtils.Plugin.GUIs.Punishments;

import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;

import java.util.UUID;

public class AddingState {
    String ID;
    UUID targetUUID;
    PunishmentType type;
    PunishmentScopes scope;
    String reason;
    long duration;

    public AddingState(String ID, UUID targetUUID, PunishmentType type, PunishmentScopes scope, String reason, long duration) {
        this.ID = ID;
        this.targetUUID = targetUUID;
        this.type = type;
        this.scope = scope;
        this.reason = reason;
        this.duration = duration;
    }
}
