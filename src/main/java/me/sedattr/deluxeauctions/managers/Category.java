package me.sedattr.deluxeauctions.managers;

import lombok.Getter;
import lombok.Setter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.MaterialHelper;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Setter
@Getter
public class Category {
    private String name;
    private ItemStack item;
    private List<String> description;
    private int slot;
    private List<String> items = new ArrayList<>();
    private ItemStack glass;
    private boolean global;
    private int priority;
    private int itemAmount = 0;

    public Category(String name, ItemStack item, ConfigurationSection section) {
        this.name = name;
        this.item = item;
        this.description = section.getStringList("description");
        this.slot = section.getInt("slot") - 1;
        this.global = section.getBoolean("global", false);
        this.priority = section.getInt("priority", 100);

        loadGlass();
        if (!this.global)
            loadItems();
    }

    private void loadGlass() {
        ItemStack glassItem = DeluxeAuctions.getInstance().normalItems.get("glass");
        if (glassItem == null)
            return;

        this.glass = glassItem.clone();
        ConfigurationSection glassSection = DeluxeAuctions.getInstance().categoriesFile.getConfigurationSection(name + ".glass");
        if (glassSection == null)
            return;

        String material = glassSection.getString("material");
        if (material == null || material.isEmpty())
            return;

        Material mat = MaterialHelper.getMaterial(material);
        if (mat == null)
            DeluxeAuctions.getInstance().dataHandler.debug(material + " %level_color%is wrong material in main menu!");
        else {
            this.glass.setType(mat);
            this.glass.setDurability((short) glassSection.getInt("data"));
        }
    }

    public void loadItems() {
        List<String> items = DeluxeAuctions.getInstance().categoriesFile.getStringList(this.name + ".items");
        if (items.isEmpty())
            return;

        List<String> newItems = new ArrayList<>();

        List<String> tools = new ArrayList<>(Arrays.asList("WOODEN", "WOOD", "STONE", "IRON", "GOLDEN", "GOLD", "DIAMOND", "NETHERITE"));
        List<String> armors = new ArrayList<>(Arrays.asList("LEATHER", "CHAINMAIL", "IRON", "GOLDEN", "GOLD", "DIAMOND", "NETHERITE"));
        List<String> armorTypes = new ArrayList<>(Arrays.asList("BOOTS", "CHESTPLATE", "LEGGINGS", "HELMET"));
        List<String> toolTypes = new ArrayList<>(Arrays.asList("SPADE", "HOE", "SHOVEL", "PICKAXE", "AXE"));
        for (String item : items) {
            if (item.equalsIgnoreCase("all_potions")) {
                newItems.addAll(Arrays.asList("LINGERING_POTION", "SPLASH_POTION", "POTION"));
                continue;
            }

            if (item.equalsIgnoreCase("all_swords")) {
                for (String tool : tools)
                    newItems.add(tool + "_SWORD");
                continue;
            }

            if (item.equalsIgnoreCase("all_tools")) {
                for (String tool : tools)
                    for (String type : toolTypes)
                        newItems.add(tool + "_" + type);
                continue;
            }

            if (item.equalsIgnoreCase("all_armors")) {
                for (String armor : armors)
                    for (String type : armorTypes)
                        newItems.add(armor + "_" + type);
                continue;
            }

            newItems.add(item);
        }

        this.items = newItems;
    }
}
