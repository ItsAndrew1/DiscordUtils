//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;

import java.lang.reflect.Method;
import java.util.Locale;

// Resolves a namespaced sound key on Paper 1.16.5 (enum) and on 1.21.3+ (registry).
public final class Sounds {
    private Sounds() {
    }

    public static Sound fromKey(String key) {
        if (key == null || key.isBlank()) return null;
        String normalized = key.toLowerCase(Locale.ROOT).trim();

        Sound fromRegistry = fromRegistry(normalized);
        if (fromRegistry != null) return fromRegistry;

        try {
            Method valueOf = Sound.class.getMethod("valueOf", String.class);
            String enumName = normalized.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
            Object sound = valueOf.invoke(null, enumName);
            if (sound instanceof Sound resolved) return resolved;
        } catch (ReflectiveOperationException ignored) {
            // Sound is no longer an enum on this server build.
        }
        return null;
    }

    private static Sound fromRegistry(String key) {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Object sounds = registryClass.getField("SOUNDS").get(null);
            Method get = null;
            for (Method method : sounds.getClass().getMethods()) {
                if (method.getName().equals("get") && method.getParameterCount() == 1) {
                    get = method;
                    break;
                }
            }
            if (get == null) return null;
            Object sound = get.invoke(sounds, NamespacedKey.minecraft(key));
            if (sound instanceof Sound resolved) return resolved;
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
