package me.sedattr.deluxeauctions.handlers;

import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Category;
import me.sedattr.deluxeauctions.others.Logger;
import me.sedattr.deluxeauctions.others.MaterialHelper;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MenuHandler {
    public void addCustomItems(Player player, HInventory gui, ConfigurationSection section) {
        if (section == null)
            return;

        section = section.getConfigurationSection("items");
        if (section == null)
            return;

        Set<String> keys = section.getKeys(false);
        if (keys.isEmpty())
            return;

        for (String key : keys) {
            int slot = section.getInt(key + ".slot");
            if (slot <= 0)
                continue;

            ItemStack item = Utils.createItemFromSection(section.getConfigurationSection(key), null);
            if (item == null)
                continue;

            List<String> commands = section.getStringList(key + ".commands");
            if (commands.isEmpty())
                gui.setItem(slot-1, ClickableItem.empty(item));
            else
                gui.setItem(slot-1, ClickableItem.of(item, (event) -> {
                    for (String command : commands) {
                        command = command
                                .replace("%player_displayname%", player.getDisplayName())
                                .replace("%player_name%", player.getName())
                                .replace("%player_uuid%", String.valueOf(player.getUniqueId()));

                        if (command.startsWith("[close]"))
                            player.closeInventory();
                        else if (command.startsWith("[player]"))
                            player.performCommand(command
                                    .replace("[player] ", "")
                                    .replace("[player]", ""));
                        else
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                                    .replace("[console] ", "")
                                    .replace("[console]", ""));
                    }
                }));
        }
    }

    public void addNormalItems(Player player, HInventory gui, ConfigurationSection section, Category category) {
        int closeSlot = section.getInt("close");
        ItemStack close = DeluxeAuctions.getInstance().normalItems.get("close");
        if (closeSlot > 0 && close != null)
            gui.setItem(closeSlot-1, ClickableItem.of(close, (event) -> player.closeInventory()));

        List<Integer> glassSlots = section.getIntegerList("glass");
        ItemStack glass = category != null ? category.getGlass() : DeluxeAuctions.getInstance().normalItems.get("glass");
        if (glass == null)
            return;
        glass = glass.clone();

        for (int i : glassSlots)
            gui.setItem(i-1, ClickableItem.empty(glass));
    }
    public void addNormalItems(Player player, HInventory gui, ConfigurationSection section) {
        List<Integer> glassSlots = section.getIntegerList("glass");
        ItemStack glass = DeluxeAuctions.getInstance().normalItems.get("glass");
        if (glass == null)
            return;
        glass = glass.clone();

        if (!glassSlots.isEmpty())
            for (int i : glassSlots)
                gui.setItem(i-1, ClickableItem.empty(glass));

        int closeSlot = section.getInt("close");
        ItemStack close = DeluxeAuctions.getInstance().normalItems.get("close");
        if (closeSlot > 0 && close != null)
            gui.setItem(closeSlot-1, ClickableItem.of(close, (event) -> player.closeInventory()));
    }

    public HInventory createInventory(Player player, ConfigurationSection section, String type, PlaceholderUtil placeholderUtil) {
        int size = section.getInt("size", 6);

        size = size > 6 ? size / 9 : size;
        if (size <= 0)
            size = 6;

        String title = section.getString("title");
        if (type.equalsIgnoreCase("search"))
            title = DeluxeAuctions.getInstance().menusFile.getString("auctions_menu.search.title");
        if (title == null)
            title = "&cTitle is missing in config!";

        if (placeholderUtil != null) {
            Map<String, String> placeholders = placeholderUtil.getPlaceholders();
            if (placeholders != null && !placeholders.isEmpty())
                for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                    String key = placeholder.getKey();
                    String value = placeholder.getValue();

                    title = title
                            .replace(key, value);
                }
        }

        HInventory gui = InventoryAPI.getInventoryManager()
                .setTitle(Utils.colorize(title.length() > 32 ? title.substring(0, 29) + "..." : title))
                .setSize(size)
                .setId(type)
                .create();

        if (!section.getName().equalsIgnoreCase("auctions_menu"))
            addNormalItems(player, gui, section);
        addCustomItems(player, gui, section);
        return gui;
    }
}
