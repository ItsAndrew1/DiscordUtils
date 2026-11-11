package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
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
    public void onCommand(PlayerCommandPreprocessEvent event) {
        FileConfiguration botConfig = plugin.botFile().getConfig();

        boolean toggleDcBot = botConfig.getBoolean("toggle-discord-bot", false);
        if(!toggleDcBot) return;

        String commandLogChannelId = botConfig.getString("command-logging.channel-id", null);
        if(commandLogChannelId == null || commandLogChannelId.isEmpty()) return;
        if(!botConfig.getBoolean("command-logging.toggle", false)) return;

        List<String> dontLogCommands = botConfig.getStringList("command-logging.not-tracking-these-commands");
        String[] command = event.getMessage().substring(1).split(" ");
        if(command.length == 0 || dontLogCommands.contains(command[0])) return;

        long commandLogID = botConfig.getLong("command-logging.channel-id", 0);
        if(commandLogID == 0){
            plugin.getLogger().warning("[!] You have command logging turned on, but the channel id is null!");
            return;
        }

        int redValue = botConfig.getInt("command-logging.embed-color.RED");
        int greenValue = botConfig.getInt("command-logging.embed-color.GREEN");
        int blueValue = botConfig.getInt("command-logging.embed-color.BLUE");
        String title = botConfig.getString("command-logging.embed-title", "COMMAND RAN BY **%player_name%**")
                .replace("%player_name%", event.getPlayer().getName());
        String description = botConfig.getString("command-logging.embed-description", "**Command Executed**: %command%")
                .replace("%command%", event.getMessage());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sendLog(commandLogID, redValue, greenValue, blueValue, title, description));
    }

    private void sendLog(long commandLogID, int red, int green, int blue, String title, String description) {
        try{
            BotMain bot = plugin.getDiscordBot();
            if(bot == null || bot.getJda() == null) return;

            Guild dcServer = bot.getDiscordServer();
            if(dcServer == null) return;

            TextChannel commandLoggingChannel = dcServer.getTextChannelById(commandLogID);
            if(commandLoggingChannel == null) {
                plugin.getLogger().warning("[!] The command logging channel couldn't be found!");
                return;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.fromRGB(red, green, blue).asRGB());
            eb.setTitle(title);
            eb.setDescription(description);
            commandLoggingChannel.sendMessageEmbeds(eb.build()).queue();
        } catch (Exception e){
            plugin.getLogger().warning("Couldn't send a Command Log. See message: "+e.getMessage());
        }
    }
}
