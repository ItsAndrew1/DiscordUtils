package me.andrew.DiscordUtils.Caching;

import me.andrew.DiscordUtils.DiscordBot.AppealState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;

import java.util.UUID;

public record Punishment(String id, UUID uuid, PunishmentType punishmentType, PunishmentScopes scope, String reason, String staff ,
                         long createdAt, long expiresAt, boolean active, boolean removed, Long removedAt, AppealState appeal_state) {

}
