package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;

import java.util.UUID;

public class PaginationState {
    UUID targetUUID;
    PunishmentsFilter filter;
    int page;
    boolean self;

    public PaginationState(UUID targetUUID, PunishmentsFilter filter, int page, boolean self) {
        this.targetUUID = targetUUID;
        this.filter = filter;
        this.page = page;
        this.self = self;
    }
}
