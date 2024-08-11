package me.sedattr.deluxeauctions.inventoryapi.inventory;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import org.bukkit.entity.Player;

public class InventoryVariables {
    @Getter private static final Map<Player, HInventory> playerInventory = new HashMap<>();
    private static final Map<Player, Long> cooldown = new HashMap<>();

    public static Long getCooldown(Player player) {
        return cooldown.getOrDefault(player, 0L);
    }

    public static void addCooldown(Player player, Long time) {
        cooldown.put(player, time);
    }

    public static void addPlayerInventory(Player player, HInventory inventory) {
        playerInventory.put(player, inventory);
    }

    public static void removePlayerInventory(Player player) {
        playerInventory.remove(player);
    }

    public static void removeCooldown(Player player) {
        cooldown.remove(player);
    }
}