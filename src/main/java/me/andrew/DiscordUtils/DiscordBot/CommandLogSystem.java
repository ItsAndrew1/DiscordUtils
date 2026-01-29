package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandLogSystem implements Listener {
    private final DiscordUtils plugin;

    public CommandLogSystem(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        FileConfiguration botConfig = plugin.botFile().getConfig();

        //Checking if the feature is toggled
        if(botConfig.getBoolean("command-logging.toggle")){
            Guild dcServer = plugin.getDiscordBot().getDiscordServer();

            //Checking if the command is one of the ones that don't need to be logged
            List<String> dontLogCommands = botConfig.getStringList("command-logging.not-tracking-these-commands");
            String[] command = e.getMessage().substring(1).split(" ");
            if(dontLogCommands.contains(command[0])) return;

            //Getting the text channel for logging
            long commandLogID = botConfig.getLong("command-logging.channel-id");
            TextChannel commandLoggingChannel = dcServer.getTextChannelById(commandLogID);

            //Building the EMBED
            EmbedBuilder eb = new EmbedBuilder();

            //Color
            int redValue = botConfig.getInt("command-logging.embed-color.RED");
            int greenValue = botConfig.getInt("command-logging.embed-color.GREEN");
            int blueValue = botConfig.getInt("command-logging.embed-color.BLUE");
            eb.setColor(Color.fromRGB(redValue, greenValue, blueValue).asRGB());

            //Title
            String title = botConfig.getString("command-logging.embed-title")
                    .replace("%player_name%", e.getPlayer().getName())
                    ;
            eb.setTitle(title);

            //Description
            String description = botConfig.getString("command-logging.embed-description")
                    .replace("%command%", e.getMessage())
                    ;
            eb.setDescription(description);

            commandLoggingChannel.sendMessageEmbeds(eb.build()).queue();
        }
    }
}
