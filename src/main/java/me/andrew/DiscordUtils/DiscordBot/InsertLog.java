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
    private final DiscordUtils plugin;

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
        this.plugin = plugin;

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
        try{
            String channelId = botConfig.getString("logs.channel-id", null);
            if(channelId == null) {
                plugin.getLogger().warning("[!] Could not send a log because the channel-id in 'botconfig.yml' is null!");
                return;
            }

            TextChannel channel = bot.getDiscordServer().getTextChannelById(channelId);
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);

            //Building the embed
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN.getRGB());
            if(!type.isWarning() && type != PunishmentType.KICK) eb.setFooter("Punishment ID: "+ID);

            String title = type.getName();
            eb.setTitle(title +" FOR "+targetPlayer.getName());

            String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                    "\n\n**Scope:** "+scope.name()+
                    "\n**Issued By:** "+staffIGN+
                    "\n**Reason:** "+reason;
            if(!type.isPermanent() && !type.isWarning()) description += "\n\n**Expires At:** "+getFormattedTime(duration+System.currentTimeMillis());

            eb.setDescription(description);
            channel.sendMessageEmbeds(eb.build()).queue();
        } catch (Exception e){
            plugin.getLogger().warning("Couldn't send a punishment log. See message: "+e.getMessage());
        }
    }

    private boolean isBotConfigured(){
        String guildId = plugin.botFile().getConfig().getString("guild-id");
        String botToken = plugin.botFile().getConfig().getString("bot-token");

        return guildId != null && botToken != null;
    }

    private void multipleChannels(){
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);

        //Checking if the bot is toggled
        boolean toggleDcBot = botConfig.getBoolean("toggle-discord-bot", false);
        boolean openDiscordBot = plugin.getConfig().getBoolean("open-discord-bot", false);
        if(!toggleDcBot || !openDiscordBot || !isBotConfigured()) return;

        if(type == PunishmentType.KICK){
            try{
                String kickChannelId = botConfig.getString("logs.multiple-channel-id.kicks-channel-id", null);
                if(kickChannelId == null) {
                    plugin.getLogger().warning("[!] Could not send a kick log because the kicks-channel-id in 'botconfig.yml' is null!");
                    return;
                }

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
            } catch (Exception e){
                plugin.getLogger().warning("Couldn't send a punishment log. See message: "+e.getMessage());
            }
        }

        if(type.isWarning()){
            try{
                String warnsChannelID = botConfig.getString("logs.multiple-channel-id.warnings-channel-id", null);
                if(warnsChannelID == null) {
                    plugin.getLogger().warning("[!] Could not send a warning log because the warnings-channel-id in 'botconfig.yml' is null!");
                    return;
                }

                TextChannel channel = bot.getDiscordServer().getTextChannelById(warnsChannelID);

                //Building the Embed
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.CYAN.getRGB());

                String title = type.getName();
                eb.setTitle(title+" FOR "+targetPlayer.getName());

                String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                        "\n\n**Scope:** "+scope.name()+
                        "\n**Issued By:** "+staffIGN+
                        "\n**Reason:** "+reason;
                eb.setDescription(description);
                channel.sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e){
                plugin.getLogger().warning("Couldn't send a punishment log. See message: "+e.getMessage());
            }
        }

        if(type == PunishmentType.PERM_BAN || type == PunishmentType.TEMP_BAN){
            try{
                String bansChannelID = botConfig.getString("logs.multiple-channel-id.bans-channel-id", null);
                if(bansChannelID == null) {
                    plugin.getLogger().warning("[!] Could not send a ban log because the bans-channel-id in 'botconfig.yml' is null!");
                    return;
                }

                TextChannel channel = bot.getDiscordServer().getTextChannelById(bansChannelID);

                //Building the Embed
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.CYAN.getRGB());
                eb.setFooter("Ban ID: "+ID);

                String title = type.getName();
                eb.setTitle(title +" FOR "+targetPlayer.getName());

                String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                        "\n\n**Scope:** "+scope.name()+
                        "\n**Issued By:** "+staffIGN+
                        "\n**Reason:** "+reason;
                if(type == PunishmentType.TEMP_BAN) description+="\n\n**Expires At:** "+getFormattedTime(duration+System.currentTimeMillis());
                eb.setDescription(description);

                channel.sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e){
                plugin.getLogger().warning("Couldn't send a punishment log. See message: "+e.getMessage());
            }
        }

        if(type == PunishmentType.PERM_MUTE || type == PunishmentType.TEMP_MUTE){
            try{
                String mutesChannelID = botConfig.getString("logs.multiple-channel-id.mutes-channel-id", null);
                if(mutesChannelID == null) {
                    plugin.getLogger().warning("[!] Could not send a mute log because the mute-channel-id in 'botconfig.yml' is null!");
                    return;
                }

                TextChannel channel = bot.getDiscordServer().getTextChannelById(mutesChannelID);

                //Building the Embed
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.CYAN.getRGB());
                eb.setFooter("Mute ID: "+ID);

                String title = type.getName();
                eb.setTitle(title+" FOR "+targetPlayer.getName());

                String description = "**Issued At:** "+getFormattedTime(lastInteraction)+
                        "\n\n**Scope:** "+scope.name()+
                        "\n**Issued By:** "+staffIGN+
                        "\n**Reason:** "+reason;
                if(type == PunishmentType.TEMP_MUTE) description+="\n\n**Expires At:** "+getFormattedTime(duration+System.currentTimeMillis());
                eb.setDescription(description);

                channel.sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e){
                plugin.getLogger().warning("Couldn't send a punishment log. See message: "+e.getMessage());
            }
        }
    }

    private String getFormattedTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        return time.format(formatter);
    }
}
