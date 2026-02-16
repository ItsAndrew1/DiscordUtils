//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.AddingState;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentsApply.PunishmentType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class InsertLog {
    private final BotMain bot;
    private final FileConfiguration botConfig;

    //Necessary data
    private final PunishmentType type;
    private final PunishmentScopes scope;
    private final String ID;
    private final UUID targetUUID;
    private final String staffIGN;
    private final String reason;
    private final long duration;
    private final long lastInteraction;

    public InsertLog(DiscordUtils plugin, BotMain bot, AddingState state) {
        this.bot = bot;

        //Getting the botConfig file
        this.botConfig = plugin.botFile().getConfig();

        //Assigning the necessary data
        this.type = state.type;
        this.scope = state.scope;
        this.ID = state.ID;
        this.targetUUID = state.targetUUID;
        this.staffIGN = state.staffIgn;
        this.reason = state.reason;
        this.duration = state.duration;
        this.lastInteraction = state.lastInteraction;

        boolean useOneChannel = botConfig.getBoolean("logs.use-only-one-channel");
        if(useOneChannel) onlyOneChannel();
        else multipleChannels();
    }

    private void onlyOneChannel(){
        String channelId = botConfig.getString("logs.channel-id");
        TextChannel channel = bot.getDiscordServer().getTextChannelById(channelId);
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);

        //Building the embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.CYAN.getRGB());
        if(!isAWarn(type) && type != PunishmentType.KICK) eb.setFooter("Punishment ID: "+ID);

        String title = switch(type){
            case PERM_BAN -> "PERMANENT BAN";
            case TEMP_BAN -> "TEMPORARY BAN";
            case PERM_BAN_WARN ->  "PERMANENT BAN WARN";
            case TEMP_BAN_WARN ->  "TEMPORARY BAN WARN";
            case KICK ->  "KICK";
            case PERM_MUTE ->  "PERMANENT MUTE";
            case TEMP_MUTE ->  "TEMPORARY MUTE";
            case PERM_MUTE_WARN ->  "PERMANENT MUTE WARN";
            case TEMP_MUTE_WARN ->  "TEMPORARY MUTE WARN";
        };
        eb.setTitle(title +" FOR "+targetPlayer.getName());

        String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                "\n\n**Scope:** "+scope.name()+
                "\n**Issued By:** "+staffIGN+
                "\n**Reason:** "+reason;
        if(!type.isPermanent() && !isAWarn(type)) description += "\n\n**Expires At:** "+getFormattedTime(duration+System.currentTimeMillis());

        eb.setDescription(description);
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void multipleChannels(){
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);

        if(type == PunishmentType.KICK){
            String kickChannelId = botConfig.getString("logs.multiple-channel-id.kicks-channel-id");
            TextChannel channel = bot.getDiscordServer().getTextChannelById(kickChannelId);

            //Building the Embed
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN.getRGB());
            eb.setTitle("KICK FOR "+targetPlayer.getName());

            String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                    "\n\n**Scope:** "+scope.name()+
                    "\n**Issued By:** "+staffIGN+
                    "\n**Reason:** "+reason;
            eb.setDescription(description);
            channel.sendMessageEmbeds(eb.build()).queue();
        }

        if(isAWarn(type)){
            String warnsChannelID = botConfig.getString("logs.multiple-channel-id.warnings-channel-id");
            TextChannel channel = bot.getDiscordServer().getTextChannelById(warnsChannelID);

            //Building the Embed
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN.getRGB());

            String title = switch (type){
                case PERM_BAN_WARN -> "PERMANENT BAN WARNING";
                case PERM_MUTE_WARN -> "PERMANENT MUTE WARNING";
                case TEMP_MUTE_WARN -> "TEMPORARY MUTE WARNING";
                case TEMP_BAN_WARN -> "TEMPORARY BAN WARNING";
                default -> null;
            };
            eb.setTitle(title+" FOR "+targetPlayer.getName());

            String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                    "\n\n**Scope:** "+scope.name()+
                    "\n**Issued By:** "+staffIGN+
                    "\n**Reason:** "+reason;
            eb.setDescription(description);
            channel.sendMessageEmbeds(eb.build()).queue();
        }

        if(type == PunishmentType.PERM_BAN || type == PunishmentType.TEMP_BAN){
            String bansChannelID = botConfig.getString("logs.multiple-channel-id.bans-channel-id");
            TextChannel channel = bot.getDiscordServer().getTextChannelById(bansChannelID);

            //Building the Embed
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN.getRGB());
            eb.setFooter("Ban ID: "+ID);

            String title = switch(type){
                case PERM_BAN -> "PERMANENT BAN";
                case TEMP_BAN -> "TEMPORARY BAN";
                default -> null;
            };
            eb.setTitle(title +" FOR "+targetPlayer.getName());

            String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                    "\n\n**Scope:** "+scope.name()+
                    "\n**Issued By:** "+staffIGN+
                    "\n**Reason:** "+reason;
            if(type == PunishmentType.TEMP_BAN) description+="\n\n**Expires At:** "+getFormattedTime(duration+System.currentTimeMillis());
            eb.setDescription(description);

            channel.sendMessageEmbeds(eb.build()).queue();
        }

        if(type == PunishmentType.PERM_MUTE || type == PunishmentType.TEMP_MUTE){
            String mutesChannelID = botConfig.getString("logs.multiple-channel-id.mutes-channel-id");
            TextChannel channel = bot.getDiscordServer().getTextChannelById(mutesChannelID);

            //Building the Embed
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN.getRGB());
            eb.setFooter("Mute ID: "+ID);

            String title = switch(type){
                case PERM_MUTE -> "PERMANENT MUTE";
                case TEMP_MUTE -> "TEMPORARY MUTE";
                default -> null;
            };
            eb.setTitle(title+" FOR "+targetPlayer.getName());

            String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                    "\n\n**Scope:** "+scope.name()+
                    "\n**Issued By:** "+staffIGN+
                    "\n**Reason:** "+reason;
            if(type == PunishmentType.TEMP_MUTE) description+="\n\n**Expires At:** "+getFormattedTime(duration+System.currentTimeMillis());
            eb.setDescription(description);

            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }

    private String getFormattedTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        return time.format(formatter);
    }

    private boolean isAWarn(PunishmentType type){
        return type.name().contains("WARN");
    }
}
