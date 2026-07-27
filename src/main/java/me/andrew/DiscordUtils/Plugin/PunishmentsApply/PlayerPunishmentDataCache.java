//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin.PunishmentsApply;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//Class for caching the punishment count
public class PlayerPunishmentDataCache {
    private final Map<PunishmentType, Map<PunishmentScopes, Integer>> mainCache;

    public PlayerPunishmentDataCache() {
        this.mainCache = new HashMap<>();
    }

    public int getNrOfPunishments(PunishmentType type, PunishmentScopes scope){
        return mainCache.getOrDefault(type, Collections.emptyMap()).getOrDefault(scope, 0);
    }

    public void insertPunishment(PunishmentType type, PunishmentScopes scope){
        mainCache.computeIfAbsent(type, m -> new HashMap<>()).merge(scope, 1, Integer::sum);
    }

    public void removePunishment(PunishmentType type, PunishmentScopes scope){
        mainCache.computeIfPresent(type, (key, submap) -> {
            submap.computeIfPresent(scope, (s, count) -> {
                int newCount = count - 1;
                return newCount > 0 ? newCount : null;
            });
            return submap.isEmpty() ? null : submap;
        });
    }
}
