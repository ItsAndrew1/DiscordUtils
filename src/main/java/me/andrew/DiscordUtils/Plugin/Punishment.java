//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import java.util.UUID;

public class Punishment{
    private final PunishmentType punishmentType;
    private final int id;
    private final UUID uuid;
    private final String reason;
    private final String staff;
    private final PunishmentScopes scope;
    private final long issuedAt;
    private final long expiresAt;
    private final boolean active;
    private final boolean removed;
    private final long removedAt;


    public Punishment(PunishmentType type, int id,  UUID uuid, PunishmentScopes scope, String reason, String staff, long issuedAt, long expiresAt, boolean active, boolean removed, long removedAt) {
        this.punishmentType = type;
        this.id = id;
        this.uuid = uuid;
        this.reason = reason;
        this.staff = staff;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.scope = scope;
        this.removed = removed;
        this.removedAt = removedAt;
    }

    //Getters
    public PunishmentType getPunishmentType() {
        return punishmentType;
    }
    public int getId() {
        return id;
    }
    public UUID getUuid() {
        return uuid;
    }
    public String getReason() {
        return reason;
    }
    public String getStaff() {
        return staff;
    }
    public PunishmentScopes getScope() {
        return scope;
    }
    public long getIssuedAt() {
        return issuedAt;
    }
    public long getExpiresAt() {
        return expiresAt;
    }
    public long getRemovedAt() {
        return removedAt;
    }
    public boolean isActive() {
        return active;
    }
    public boolean isRemoved() {
        return removed;
    }
}
