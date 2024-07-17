package me.sedattr.deluxeauctions.handlers;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlacklistHandler {
    public List<String> blacklistedNames = List.of();
    public List<String> blacklistedMaterials = List.of();
    public List<Integer> blacklistedModels = List.of();
    public List<List<String>> blacklistedLores = List.of();

    public BlacklistHandler() {
        loadLores();
        loadMaterials();
        loadModels();
        loadNames();
    }

    public boolean isBlacklisted(ItemStack item) {
        if (this.blacklistedMaterials.contains(item.getType().name()))
            return true;

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null)
            return false;

        int model;
        if (DeluxeAuctions.getInstance().version > 13 && itemMeta.hasCustomModelData())
            model = itemMeta.getCustomModelData();
        else
            model = item.getDurability();

        if (model > 0 && this.blacklistedModels.contains(model))
            return true;

        String name = Utils.getDisplayName(item);
        if (name != null && !name.isEmpty() && this.blacklistedNames.contains(name))
            return true;

        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.isEmpty())
            return false;

        List<String> newLore = new ArrayList<>(lore.size());
        for (String line : lore)
            newLore.add(Utils.colorize(line));

        return this.blacklistedLores.contains(newLore);
    }

    public void loadLores() {
        ConfigurationSection section = DeluxeAuctions.getInstance().itemsFile.getConfigurationSection("blacklisted_items.lores");
        if (section == null || !section.getBoolean("enabled"))
            return;

        section = section.getConfigurationSection("list");
        if (section == null)
            return;

        Set<String> lores = section.getKeys(false);
        if (lores.isEmpty())
            return;

        for (String number : lores) {
            List<String> lines = section.getStringList(number);
            if (lines.isEmpty())
                continue;

            List<String> newLines = new ArrayList<>(lines.size());
            for (String line : lines)
                newLines.add(Utils.colorize(line));

            this.blacklistedLores.add(newLines);
        }
    }

    public void loadModels() {
        ConfigurationSection section = DeluxeAuctions.getInstance().itemsFile.getConfigurationSection("blacklisted_items.models");
        if (section == null || !section.getBoolean("enabled"))
            return;

        List<Integer> models = section.getIntegerList("list");
        if (models.isEmpty())
            return;

        this.blacklistedModels.addAll(models);
    }

    public void loadMaterials() {
        ConfigurationSection section = DeluxeAuctions.getInstance().itemsFile.getConfigurationSection("blacklisted_items.material");
        if (section == null || !section.getBoolean("enabled"))
            return;

        List<String> materials = section.getStringList("list");
        if (materials.isEmpty())
            return;

        this.blacklistedMaterials.addAll(materials);
    }

    public void loadNames() {
        ConfigurationSection section = DeluxeAuctions.getInstance().itemsFile.getConfigurationSection("blacklisted_items.name");
        if (section == null || !section.getBoolean("enabled"))
            return;

        List<String> names = section.getStringList("list");
        if (names.isEmpty())
            return;

        for (String name : names) {
            this.blacklistedNames.add(Utils.colorize(name));
        }
    }
}
