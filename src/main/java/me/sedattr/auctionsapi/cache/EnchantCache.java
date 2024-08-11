package me.sedattr.auctionsapi.cache;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class EnchantCache {
    private static HashMap<String, String> enchants;

    public static void loadEnchants() {
        enchants = new HashMap<>();

        if (DeluxeAuctions.getInstance().version > 12)
            for (Enchantment enchantment : Registry.ENCHANTMENT) {
                String name = enchantment.getKey().getKey();
                enchants.put(name, name.replace("_", " "));
            }
        else {
            List<String> names = new ArrayList<>(Arrays.asList(
                    "arrow_damage",
                    "arrow_fire",
                    "arrow_infinite",
                    "arrow_knockback",
                    "binding_curse",
                    "damage_all",
                    "damage_arthropods",
                    "damage_undead",
                    "depth_strider",
                    "dig_speed",
                    "durability",
                    "fire_aspect",
                    "frost_walker",
                    "knockback",
                    "loot_bonus_blocks",
                    "loot_bonus_mobs",
                    "luck",
                    "lure",
                    "mending",
                    "oxygen",
                    "protection_environmental",
                    "protection_explosions",
                    "protection_fall",
                    "protection_fire",
                    "protection_projectile",
                    "silk_touch",
                    "sweeping_edge",
                    "thorns",
                    "vanishing_curse",
                    "water_worker"
            ));

            List<String> newNames = new ArrayList<>(Arrays.asList(
                    "power",
                    "flame",
                    "infinity",
                    "punch",
                    "binding curse",
                    "sharpness",
                    "bane of arthropods",
                    "smite",
                    "depth strider",
                    "efficiency",
                    "unbreaking",
                    "fire aspect",
                    "frost walker",
                    "knockback",
                    "fortune",
                    "looting",
                    "luck of the sea",
                    "lure",
                    "mending",
                    "respiration",
                    "protection",
                    "blast protection",
                    "feather falling",
                    "fire protection",
                    "projectile protection",
                    "silk touch",
                    "sweeping edge",
                    "thorns",
                    "vanishing curse",
                    "aqua affinity"
            ));

            names.forEach(a -> enchants.put(a, newNames.get(names.indexOf(a))));
        }
    }

    public static boolean isEnchantmentAdded(Map<Enchantment, Integer> enchantments, String search) {
        if (enchantments == null || enchantments.isEmpty())
            return false;

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();

            String enchantmentName = DeluxeAuctions.getInstance().version > 12 ? enchantment.getKey().getKey() : enchantment.getName();
            if (enchantmentName.isEmpty())
                continue;

            String name = enchants.get(enchantmentName.toLowerCase());
            if (name == null || name.isEmpty())
                continue;

            String[] args = search.split(" ", 2);
            if (name.startsWith(args[0])) {
                if (args.length < 2)
                    return true;

                try {
                    Integer level = Integer.valueOf(args[1]);
                    return level.equals(entry.getValue());
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return false;
    }
}