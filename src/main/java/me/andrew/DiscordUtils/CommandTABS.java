package me.andrew.DiscordUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandTABS implements TabCompleter {
    DiscordUtils plugin;

    public CommandTABS(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NonNull @NotNull String[] strings) {
        if(command.getName().equalsIgnoreCase("dcutils")) {
            if(strings.length == 1){
                return Arrays.asList("setdclink", "setdcchoice", "reload", "help");
            }
            if(strings.length == 2 && strings[0].equalsIgnoreCase("setdcchoice")) {
                return Arrays.asList("book", "chat-message");
            }
        }

        return Collections.emptyList();
    }
}
