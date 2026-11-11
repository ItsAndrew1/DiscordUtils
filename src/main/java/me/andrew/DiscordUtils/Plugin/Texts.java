//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

//Legacy chat helpers that exist on Spigot 1.16.5 (no Adventure API).
public final class Texts {
    private Texts() {
    }

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> lines(String... lines) {
        List<String> lore = new ArrayList<>(lines.length);
        for (String line : lines) lore.add(color(line));
        return lore;
    }

    public static BaseComponent[] linkify(String legacyLine, String word, String url, String hover) {
        String colored = color(legacyLine);
        if (word == null || word.isEmpty() || !colored.contains(word)) {
            return TextComponent.fromLegacyText(colored);
        }

        ComponentBuilder builder = new ComponentBuilder("");
        int start = 0;
        while (true) {
            int index = colored.indexOf(word, start);
            if (index < 0) break;
            append(builder, colored.substring(start, index), null, null);
            append(builder, word, url, hover);
            start = index + word.length();
        }
        append(builder, colored.substring(start), null, null);
        return builder.create();
    }

    private static void append(ComponentBuilder builder, String text, String url, String hover) {
        if (text == null || text.isEmpty()) return;
        BaseComponent[] parts = TextComponent.fromLegacyText(text);
        if (url != null) {
            ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover == null ? url : hover));
            for (BaseComponent part : parts) {
                part.setClickEvent(click);
                part.setHoverEvent(hoverEvent);
            }
        }
        builder.append(parts);
    }
}
