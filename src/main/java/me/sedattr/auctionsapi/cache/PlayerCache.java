package me.sedattr.auctionsapi.cache;

import lombok.Getter;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.managers.PlayerStats;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class PlayerCache {
    @Getter private static final HashMap<UUID, PlayerStats> stats = new HashMap<>();
    @Getter private static final HashMap<UUID, PlayerPreferences> players = new HashMap<>();
    @Getter private static final HashMap<UUID, ItemStack> items = new HashMap<>();

    public static PlayerStats getStats(UUID player) {
        PlayerStats playerStats = stats.get(player);
        if (playerStats == null) {
            playerStats = new PlayerStats(player);

            stats.put(player, playerStats);
            return playerStats;
        }
        return playerStats;
    }

    public static ItemStack getItem(UUID player) {
        return items.getOrDefault(player, null);
    }

    public static void setItem(UUID player, ItemStack item) {
        if (item == null)
            items.remove(player);
        else
            items.put(player, item);
    }

    public static PlayerPreferences getPreferences(UUID player) {
        PlayerPreferences preferences = players.get(player);
        if (preferences == null) {
            preferences = new PlayerPreferences(player);

            players.put(player, preferences);
            return preferences;
        }
        return preferences;
    }

    public static void removeStats(UUID player) {
        stats.remove(player);
    }

    public static void removeItem(UUID player) {
        items.remove(player);
    }

    public static void removePreferences(UUID player) {
        players.remove(player);
    }
}