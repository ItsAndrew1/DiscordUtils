package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import java.util.UUID;

public class AddingState {
    public String ID;
    public UUID targetUUID;
    public PunishmentType type;
    public PunishmentScopes scope;
    public String reason;
    public long duration;
    public long lastInteraction;

    public AddingState(String ID, UUID targetUUID, PunishmentType type, PunishmentScopes scope, String reason, long duration,  long lastInteraction) {
        this.ID = ID;
        this.targetUUID = targetUUID;
        this.type = type;
        this.scope = scope;
        this.reason = reason;
        this.duration = duration;
        this.lastInteraction = lastInteraction;
    }
}
