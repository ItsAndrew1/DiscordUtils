package me.andrew.DiscordUtils.Caching;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationCodesCaching {
    private final Map<UUID, VerificationCode> codesCache = new ConcurrentHashMap<>();
    private final Map<String, UUID> discordCodeCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidDiscordIdCache = new ConcurrentHashMap<>();

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

}
