//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

//Helper class to automatically delete the messages the users may send in those channels.
public class MessageDeleteSystem extends ListenerAdapter {
    private final DiscordUtils plugin;

    public MessageDeleteSystem(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event){
        if(event.getAuthor().isBot()) return;

        FileConfiguration botConfig = plugin.botFile().getConfig();
        Guild dcServer = plugin.getDiscordBot().getDiscordServer();

        long bannedUsersChannelID = botConfig.getLong("banned-users-channel.id");
        long verifyChannelID = botConfig.getLong("verification.verify-channel-id");
        if(bannedUsersChannelID != -1 && verifyChannelID != -1){
            TextChannel bannedUsersChannel = dcServer.getTextChannelById(bannedUsersChannelID);
            TextChannel verifyChannel = dcServer.getTextChannelById(verifyChannelID);

            //Checking if it is the right channel
            if(!event.getChannel().equals(bannedUsersChannel) && !event.getChannel().equals(verifyChannel)) return;

            event.getMessage().delete().queue();
        }
    }
}
