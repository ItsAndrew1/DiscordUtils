//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.Bukkit;
import org.bukkit.block.Skull;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

//Applies a base64 skin texture without calling Paper-only profile APIs.
//Paper's setPlayerProfile is used when it exists; Spigot falls back to GameProfile reflection.
public final class SkullTextures {
    private SkullTextures() {
    }

    public static void apply(SkullMeta meta, String texture) {
        if (meta == null || texture == null || texture.isEmpty()) return;
        if (applyPaper(meta, texture)) return;
        applyGameProfile(meta, texture);
    }

    public static void apply(Skull skull, String texture) {
        if (skull == null || texture == null || texture.isEmpty()) return;
        if (applyPaper(skull, texture)) {
            skull.update(true, false);
            return;
        }

        try {
            Object profile = gameProfile(texture);
            Object tile = tileEntity(skull);
            if (tile != null) {
                Method setter = tile.getClass().getMethod("setGameProfile", profile.getClass());
                setter.invoke(tile, profile);
            } else {
                applyGameProfile(skull, texture);
            }
        } catch (ReflectiveOperationException ignored) {
            applyGameProfile(skull, texture);
        }
        skull.update(true, false);
    }

    private static boolean applyPaper(Object target, String texture) {
        try {
            Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            Class<?> propertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            Object profile = Bukkit.class.getMethod("createProfile", UUID.class).invoke(null, UUID.randomUUID());
            Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", texture);
            profile.getClass().getMethod("setProperty", propertyClass).invoke(profile, property);
            target.getClass().getMethod("setPlayerProfile", profileClass).invoke(target, profile);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object tileEntity(Skull skull) {
        Class<?> current = skull.getClass();
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod("getTileEntity");
                method.setAccessible(true);
                return method.invoke(skull);
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void applyGameProfile(Object target, String texture) {
        try {
            Object profile = gameProfile(texture);
            Field field = findField(target.getClass(), "profile");
            if (field == null) return;
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.isInstance(profile)) {
                field.set(target, profile);
                return;
            }
            Object wrapped = wrapResolvable(type, profile);
            if (wrapped != null) field.set(target, wrapped);
        } catch (ReflectiveOperationException ignored) {
            // Texture is optional; the item or block still works without a custom skin.
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Object gameProfile(String texture) throws ReflectiveOperationException {
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "");
        Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", texture);
        Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
        properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);
        return profile;
    }

    private static Object wrapResolvable(Class<?> type, Object profile) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != 1 || !params[0].isInstance(profile)) continue;
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(profile);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }
}
