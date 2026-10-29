//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Caching;

import me.andrew.DiscordUtils.DiscordBot.AppealState;
import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//Class for caching the punishment count
public class PlayerPunishmentDataCache {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache;
    private final DiscordUtils plugin;

    public PlayerPunishmentDataCache(DiscordUtils plugin) {
        this.mainCache = new ConcurrentHashMap<>();
        this.plugin = plugin;
    }

    //Helper Methods
    public void addPlayerPunishments(UUID playerUUID, CopyOnWriteArrayList<Punishment> punishments){
        //This will run only once, when the server opens up, so no need for other checks.
        mainCache.put(playerUUID, punishments);
    }

    public ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> getMainCache() {
        return mainCache;
    }

    public void addPunishment (UUID playerUUID, Punishment punishment) {
        mainCache.computeIfAbsent(playerUUID, k -> new CopyOnWriteArrayList<>()).add(punishment);
    }

    public void removePunishment(UUID playerUUID, Punishment punishment) {
        if(punishment == null) return;

        Punishment newPunishment = new Punishment(punishment.id(), punishment.uuid(), punishment.punishmentType(), punishment.scope(), punishment.reason(),
                punishment.staff(), punishment.createdAt(), punishment.expiresAt(), false, true, System.currentTimeMillis(), punishment.appeal_state()
        );
        modifyPunishment(playerUUID, newPunishment);
    }

    public void setPunishmentAppealState(Punishment punishment, AppealState state){
        Punishment newPunishment = new Punishment(punishment.id(), punishment.uuid(), punishment.punishmentType(), punishment.scope(),
                punishment.reason(), punishment.staff(), punishment.createdAt(), punishment.expiresAt(), punishment.active(), punishment.removed(),
                punishment.removedAt(), state
        );

        modifyPunishment(punishment.uuid(), newPunishment);
    }

    public boolean playerHasPunishments(UUID playerUUID){
        List<Punishment> punishments = mainCache.get(playerUUID);
        return punishments != null && punishments.stream().anyMatch(Punishment::active);
    }

    public int getNrOfPunishments(UUID playerUUID, boolean active){
        int count = 0;
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return 0;

        for(Punishment punishment : punishments){
            if(punishment.active() == active) count++;
        }

        return count;
    }

    public CopyOnWriteArrayList<Punishment> getPlayerPunishments(UUID playerUUID, PunishmentsFilter filter, int limit, int offset) {
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return new CopyOnWriteArrayList<>();

        List<Punishment> sortedPunishments = punishments.stream().filter(p -> {
            switch (filter){
                case EXPIRED -> {
                    return !p.active();
                }
                case ACTIVE -> {
                    return p.active();
                }
            }
            return true;
        }).sorted(Comparator.comparingLong(Punishment::createdAt)).skip(offset).limit(limit).toList();

        return new CopyOnWriteArrayList<>(sortedPunishments);
    }

    public Punishment getPunishment (UUID playerUUID, PunishmentType type){
        List<Punishment> playerPunishments = mainCache.get(playerUUID);
        if(playerPunishments == null) return null;

        return playerPunishments.stream().filter(p -> p.punishmentType().equals(type) && p.active()).findFirst().orElse(null);
    }

    public void expirePunishmentById (UUID playerUUID, String ID){
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return;

        Punishment punishment = punishments.stream().filter(p -> p.id().equals(ID)).findFirst().orElse(null);
        if(punishment == null) return;

        Punishment newPunishment = new Punishment(ID, punishment.uuid(), punishment.punishmentType(), punishment.scope(),
                punishment.reason(), punishment.staff(), punishment.createdAt(), punishment.expiresAt(), false, punishment.removed(), punishment.removedAt(),
                punishment.appeal_state());

        modifyPunishment(playerUUID, newPunishment);
    }

    public boolean playerHasPunishment(UUID playerUUID, PunishmentType type, PunishmentScopes scope){
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return false;

        return punishments.stream().anyMatch(p -> p.punishmentType().equals(type) && p.scope().equals(scope) && p.active());
    }

    public boolean isPlayerBanned(UUID playerUUID, PunishmentScopes scope){
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return false;

        return punishments.stream().anyMatch(p -> p.scope().equals(scope) && (p.punishmentType().equals(PunishmentType.TEMP_BAN) || p.punishmentType().equals(PunishmentType.PERM_BAN)) && p.active());
    }

    public boolean isPlayerMuted(UUID playerUUID, PunishmentScopes scope){
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return false;

        return punishments.stream().anyMatch(p -> p.scope().equals(scope) && (p.punishmentType().equals(PunishmentType.TEMP_MUTE) || p.punishmentType().equals(PunishmentType.PERM_MUTE)) && p.active());
    }

    public boolean hasNrOfWarns(UUID playerUUID, PunishmentType warnType, PunishmentScopes warnScope){
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments == null) return false;

        long maxWarns = plugin.getConfig().getLong("warns-amount");
        long activeWarns = punishments.stream().filter(p -> p.punishmentType().equals(warnType) && p.scope().equals(warnScope) && p.active()).count();

        return activeWarns == maxWarns;
    }

    public Punishment getPunishmentByID(String ID){
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Punishment>> mainCache = plugin.getPlayerPunishmentDataCache().getMainCache();

        for(List<Punishment> list : mainCache.values()){
            if(list == null) continue;

            Punishment found = list.stream().filter(p -> p.id().equals(ID)).findFirst().orElse(null);
            if(found != null) return found;
        }

        return null;
    }

    public boolean isUserBanned(String userID, PunishmentScopes scope){
        boolean permBanned, tempBanned;
        UUID userUUID = plugin.getPlayerVerificationCache().getUuidFromDiscordId(userID);

        permBanned = plugin.getPlayerPunishmentDataCache().getMainCache().get(userUUID).stream().anyMatch(p -> p.punishmentType().equals(PunishmentType.PERM_BAN) &&  p.scope().equals(scope) && p.active());
        tempBanned = plugin.getPlayerPunishmentDataCache().getMainCache().get(userUUID).stream().anyMatch(p -> p.punishmentType().equals(PunishmentType.TEMP_BAN) &&  p.scope().equals(scope) && p.active());

        return permBanned || tempBanned;
    }

    public boolean isUserOnTimeout(String userID, PunishmentScopes scope){
        boolean permTimeout, tempTimeout;
        UUID userUUID = plugin.getPlayerVerificationCache().getUuidFromDiscordId(userID);

        permTimeout = plugin.getPlayerPunishmentDataCache().getMainCache().get(userUUID).stream().anyMatch(p -> p.punishmentType().equals(PunishmentType.PERM_MUTE) &&  p.scope().equals(scope) && p.active());
        tempTimeout = plugin.getPlayerPunishmentDataCache().getMainCache().get(userUUID).stream().anyMatch(p -> p.punishmentType().equals(PunishmentType.TEMP_MUTE) &&  p.scope().equals(scope) && p.active());

        return permTimeout || tempTimeout;
    }

    private void modifyPunishment (UUID playerUUID, Punishment punishment) {
        List<Punishment> punishments = mainCache.get(playerUUID);
        if(punishments != null){
            punishments.removeIf(p -> p.id().equals(punishment.id()));
            punishments.add(punishment);
        }
    }
}