package me.andrew.DiscordUtils.Caching;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerVerificationCache {
    private final Map<UUID, VerificationCode> codesCache = new ConcurrentHashMap<>();
    private final Map<String, UUID> discordCodeCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidDiscordIdCache = new ConcurrentHashMap<>();
    private final Map<String, UUID> discordIdUuidCache = new ConcurrentHashMap<>();

    public record VerificationCode(String code, long expireTime){
        private boolean isCodeExpired(){
            return System.currentTimeMillis() > expireTime;
        }
    }

    //Methods for main code cache
    public void createCode(UUID playerUUID, String code, long millis){
        long expiresAt = System.currentTimeMillis() + millis;
        codesCache.put(playerUUID, new VerificationCode(code, expiresAt));
    }
    public void deleteCode(UUID playerUUID){
        codesCache.remove(playerUUID);
    }
    public boolean isPlayerVerifying(UUID playerUUID){
        return codesCache.containsKey(playerUUID);
    }
    public String getCode(UUID playerUUID){
        VerificationCode codeCache = codesCache.get(playerUUID);
        if(codeCache == null) return null;

        String code = codeCache.code;

        //Checking if the code is null or has expired
        if(codeCache.isCodeExpired()){
            discordCodeCache.remove(code);
            codesCache.remove(playerUUID);
            return null;
        }

        return codeCache.code;
    }

    //Methods for the cache needed for Discord Part
    public void saveForDiscord(String code, UUID playerUUID){
        discordCodeCache.put(code, playerUUID);
    }
    public void removeFromDiscord(String code){
        discordCodeCache.remove(code);
    }
    public UUID getUuidFromCode(String code){
        return discordCodeCache.get(code);
    }

    //Methods for the UUID <-> Discord ID cache
    public void putUuidDiscordID(UUID playerUUID, String discordID){
        uuidDiscordIdCache.put(playerUUID, discordID);
    }
    public void removeUuidDiscordID(UUID playerUUID){
        uuidDiscordIdCache.remove(playerUUID);
    }
    public String getDiscordIdFromUuid(UUID playerUUID){
        return uuidDiscordIdCache.get(playerUUID);
    }

    //Methods for the Discord Id <-> UUID cache
    public void putDiscordIdUUID(UUID playerUUID, String discordID){
        discordIdUuidCache.put(discordID, playerUUID);
    }
    public void removeDiscordIdUUID(String discordID){
        discordIdUuidCache.remove(discordID);
    }
    public UUID getUuidFromDiscordId(String discordID){
        return discordIdUuidCache.get(discordID);
    }
}
