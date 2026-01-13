package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;

public class BotMain{
    private final JDA jda;
    private final Guild discordServer;
    private final PunishmentHistory punishmentHistory;
    private final AddPunishments addPunishments;

    public BotMain(String token, String guildId, DiscordUtils plugin) throws Exception{
        SlashCommands slashCommands = new SlashCommands(plugin, this);
        punishmentHistory = new PunishmentHistory(plugin);
        addPunishments = new AddPunishments(plugin, this);

        //Creating the bot itself
        this.jda = JDABuilder.createDefault(token)
                .addEventListeners(slashCommands)
                .addEventListeners(punishmentHistory)
                .addEventListeners(addPunishments)
                .build()
                .awaitReady();

        //Getting the discord server and adding commands
        this.discordServer = jda.getGuildById(guildId);
        if(discordServer == null){
            Bukkit.getLogger().info("[DISCORDUTILS] Discord server not found! Bot won't start.");
            jda.shutdownNow();
        }

        discordServer.updateCommands().addCommands(
                Commands.slash("verify", "Verify your minecraft account!")
                        .addOption(OptionType.INTEGER, "code", "Enter the code you were given.", true),
                Commands.slash("pshistory", "View the history of a player!")
                        .addOption(OptionType.STRING, "ign", "Enter the player's IGN!", false),
                Commands.slash("punish", "Punish a player. You cannot enter your own name!")
                        .addOption(OptionType.STRING, "ign", "Enter the player's IGN.", true)
        ).queue();
    }

    public JDA getJda() {
        return jda;
    }
    public Guild getDiscordServer() {
        return discordServer;
    }
    public PunishmentHistory getPunishmentHistory() {
        return punishmentHistory;
    }
    public AddPunishments getAddPunishments() {
        return addPunishments;
    }
}
