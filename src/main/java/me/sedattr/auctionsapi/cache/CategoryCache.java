package me.sedattr.auctionsapi.cache;

import com.google.common.collect.Maps;
import lombok.Getter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Category;
import me.sedattr.deluxeauctions.managers.CustomItem;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryCache {
    @Getter private static HashMap<String, Category> categories = Maps.newHashMap();
    @Getter private static int totalPage = 1;

    private static void loadTotalPages() {
        boolean addPageSlots = DeluxeAuctions.getInstance().menusFile.getBoolean("auctions_menu.category_list.pagination.add_page_slots", true);
        int categoriesSize = categories.size();

        int totalCategories = categoriesSize;
        int slotsSize = 4;

        int page = 1;
        int total = slotsSize;
        if (addPageSlots)
            total += 2;

        if (categoriesSize > total) {
            totalCategories -= slotsSize;
            if (addPageSlots)
                totalCategories--;

            while (totalCategories >= slotsSize) {
                page++;
                totalCategories-=slotsSize;
            }

            int minimum = addPageSlots ? 1 : 0;
            if (totalCategories > minimum)
                page++;
        }

        totalPage = page;
    }
    
    public static boolean isCustomItem(String item, ItemStack itemStack) {
        if (DeluxeAuctions.getInstance().ecoItemsAddon != null) {
            ItemStack ecoItem = DeluxeAuctions.getInstance().ecoItemsAddon.getEcoItem(item);
            if (ecoItem != null && ecoItem.isSimilar(itemStack))
                return true;
        }

        CustomItem customItem = DeluxeAuctions.getInstance().customItems.get(item);
        if (customItem == null)
            return false;
        if (itemStack == null)
            return true;

        String type = customItem.getType();
        List<String> lore = customItem.getLore();
        if (!lore.isEmpty()) {
            List<String> itemLore = Utils.getLore(itemStack);
            if (itemLore == null || itemLore.isEmpty())
                return false;

            if (type.equalsIgnoreCase("contains_lore"))
                return new HashSet<>(itemLore).containsAll(lore);
            else if (!lore.equals(itemLore))
                return false;
        }

        String material = customItem.getMaterial();
        if (!material.isEmpty() && !material.equals(itemStack.getType().name()))
            return false;

        String name = customItem.getName();
        if (!name.isEmpty()) {
            String itemName = Utils.getDisplayName(itemStack);
            if (itemName == null || itemName.isEmpty())
                return false;

            if (type.equalsIgnoreCase("contains_name"))
                return itemName.contains(name);
            else if (!name.equals(itemName))
                return false;
        }

        int model = customItem.getModel();
        if (model > 0) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null)
                return false;

            int itemModel;
            if (DeluxeAuctions.getInstance().version > 13 && itemMeta.hasCustomModelData())
                itemModel = itemMeta.getCustomModelData();
            else
                itemModel = itemStack.getDurability();

            return itemModel == model;
        }

        return true;
    }

    public static String getItemCategory(ItemStack itemStack) {
        if (itemStack == null)
            return "";

        for (Category category : categories.values()) {
            List<String> items = category.getItems();
            for (String item : items) {
                if (isCustomItem(item, itemStack))
                    return category.getName();

                String displayName = Utils.getDisplayName(itemStack);
                if (displayName != null && displayName.equals(item))
                    return category.getName();

                if (itemStack.getType().name().equals(item))
                    return category.getName();

                if (item.equalsIgnoreCase("all_spawn_eggs"))
                    if (itemStack.getType().name().endsWith("SPAWN_EGG"))
                        return category.getName();

                if (item.equalsIgnoreCase("all_ingots"))
                    if (itemStack.getType().name().endsWith("INGOT"))
                        return category.getName();

                if (item.equalsIgnoreCase("all_nuggets"))
                    if (itemStack.getType().name().endsWith("NUGGET"))
                        return category.getName();

                if (item.equalsIgnoreCase("all_templates"))
                    if (itemStack.getType().name().endsWith("TEMPLATE"))
                        return category.getName();

                if (item.equalsIgnoreCase("all_dyes"))
                    if (itemStack.getType().name().endsWith("DYE"))
                        return category.getName();

                if (item.equalsIgnoreCase("all_blocks"))
                    if (itemStack.getType().isBlock())
                        return category.getName();

                if (item.equalsIgnoreCase("all_consumables"))
                    if (itemStack.getType().isEdible())
                        return category.getName();
            }
        }

        String returnCategory = DeluxeAuctions.getInstance().returnCategory;
        if (!returnCategory.isEmpty())
            return returnCategory;

        return "";
    }

    public static void loadCategories() {
        categories = new HashMap<>();

        Set<String> categoryNames = DeluxeAuctions.getInstance().categoriesFile.getKeys(false);
        if (categoryNames.isEmpty())
            return;

        for (String category : categoryNames) {
            ConfigurationSection section = DeluxeAuctions.getInstance().categoriesFile.getConfigurationSection(category);
            ItemStack itemStack = Utils.createItemFromSection(section, null);
            if (itemStack == null)
                continue;

            categories.put(category, new Category(category, itemStack, section));
        }

        loadTotalPages();
    }
}