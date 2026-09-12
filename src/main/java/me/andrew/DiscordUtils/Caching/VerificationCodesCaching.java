package me.andrew.DiscordUtils.Caching;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationCodesCaching {
    private final Map<UUID, VerificationCode> codesCache = new ConcurrentHashMap<>();

    public record VerificationCode(String code, long expireTime){
        private boolean isCodeExpired(){
            return System.currentTimeMillis() > expireTime;
        }
    }

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
        VerificationCode code = codesCache.get(playerUUID);

        //Checking if the code is null or has expired
        if(code == null || code.isCodeExpired()) return null;

        return code.code;
    }
}
