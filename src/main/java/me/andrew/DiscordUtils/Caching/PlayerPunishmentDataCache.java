//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Caching;

import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//Class for caching the punishment count
public class PlayerPunishmentDataCache {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<PlayerPunishments>> mainCache;

    public PlayerPunishmentDataCache() {
        this.mainCache = new ConcurrentHashMap<>();
    }

    public record PlayerPunishments(){

    }
}
