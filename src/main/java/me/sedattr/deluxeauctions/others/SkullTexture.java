package me.sedattr.deluxeauctions.others;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public class SkullTexture {
    private Method GET_PROPERTIES;
    private Method INSERT_PROPERTY;
    private Constructor<?> GAME_PROFILE_CONSTRUCTOR;
    private Constructor<?> PROPERTY_CONSTRUCTOR;

    {
        try {
            final Class<?> gameProfile = Class.forName("com.mojang.authlib.GameProfile");
            final Class<?> property = Class.forName("com.mojang.authlib.properties.Property");
            final Class<?> propertyMap = Class.forName("com.mojang.authlib.properties.PropertyMap");
            GAME_PROFILE_CONSTRUCTOR = getConstructor(gameProfile, 2);
            PROPERTY_CONSTRUCTOR = getConstructor(property, 2);
            GET_PROPERTIES = getMethod(gameProfile, "getProperties");
            INSERT_PROPERTY = getMethod(propertyMap, "put");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Method getMethod(final Class<?> clazz, final String name) {
        for (final Method m : clazz.getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private Field getField(final Class<?> clazz, final String fieldName) throws NoSuchFieldException {
        return clazz.getDeclaredField(fieldName);
    }

    public void setFieldValue(final Object object, final String fieldName, final Object value) throws NoSuchFieldException, IllegalAccessException {
        final Field f = getField(object.getClass(), fieldName);
        f.setAccessible(true);
        f.set(object, value);
    }

    public Constructor<?> getConstructor(final Class<?> clazz, final int numParams) {
        for (final Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterTypes().length == numParams) {
                return constructor;
            }
        }
        return null;
    }

    public ItemStack getOldSkull(ItemStack item, String texture) {
        final ItemMeta meta = item.getItemMeta();
        try {
            final Object profile = GAME_PROFILE_CONSTRUCTOR.newInstance(UUID.randomUUID(), UUID.randomUUID().toString().substring(17).replace("-", ""));
            final Object properties = GET_PROPERTIES.invoke(profile);
            INSERT_PROPERTY.invoke(properties, "textures", PROPERTY_CONSTRUCTOR.newInstance("textures", texture));
            setFieldValue(meta, "profile", profile);
        } catch (Exception e) {
            return item;
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getSkull(Material material, String texture) {
        if (material == null)
            return null;

        ItemStack item;
        if (DeluxeAuctions.getInstance().version > 12)
            item = new ItemStack(material, 1);
        else
            item = new ItemStack(material, 1, (short) 3);
        if (texture == null || texture.isEmpty())
            return item;

        texture = texture.replace(" ", "");
        if (texture.length() <= 16) {
            if (texture.startsWith("hdb-")) {
                try {
                    return DeluxeAuctions.getInstance().headDatabase.getHdbAPI().getItemHead(texture.replace("hdb-", ""));
                } catch (Exception ignored) {
                    return item;
                }
            }

            SkullMeta sm = (SkullMeta) item.getItemMeta();
            sm.setOwner(texture);

            item.setItemMeta(sm);
            return item;
        } else {
            if (DeluxeAuctions.getInstance().version >= 21) {
                try {
                    String json = new String(Base64.getDecoder().decode(texture));
                    JsonObject textureJson = JsonParser.parseString(json).getAsJsonObject();

                    String url = textureJson.getAsJsonObject("textures")
                            .getAsJsonObject("SKIN")
                            .get("url").getAsString();

                    URL skinUrl = new URL(url);
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                    profile.getTextures().setSkin(skinUrl);
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    meta.setOwnerProfile(profile);
                    item.setItemMeta(meta);
                } catch (Exception e) {
                    return getOldSkull(item, texture);
                }
            } else {
                return getOldSkull(item, texture);
            }
        }

        return item;
    }
}