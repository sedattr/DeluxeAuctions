package me.sedattr.deluxeauctions.menus;

import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.managers.Economy;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EconomyMenu {
    private final Player player;
    private final ConfigurationSection section;
    private HInventory gui;
    private final PlayerPreferences playerAuction;

    public EconomyMenu(Player player) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("economy_menu");
        this.playerAuction = PlayerCache.getPreferences(player.getUniqueId());
    }

    public void open() {
        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "duration", null);

        int goBackSlot = this.section.getInt("back");
        ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
        if (goBackSlot > 0 && goBackItem != null)
            gui.setItem(goBackSlot, ClickableItem.of(goBackItem, (event) -> new CreateMenu(this.player).open("main")));

        loadEconomyItems();

        this.gui.open(this.player);
    }

    private void loadEconomyItems() {
        for (Economy economy : DeluxeAuctions.getInstance().economies.values()) {
            if (!economy.isEnabled())
                continue;

            PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                    .addPlaceholder("%economy_name%", economy.getName());

            this.gui.setItem(economy.getSlot(), ClickableItem.of(economy.getItem(), (event) -> {
                if (this.playerAuction.getCreateEconomy().getKey().equals(economy.getKey())) {
                    Utils.sendMessage(player, "already_selected_economy", placeholderUtil);
                    return;
                }

                if (!economy.getPermission().isEmpty() && !player.hasPermission(economy.getPermission())) {
                    Utils.sendMessage(player, "no_permission_for_economy", placeholderUtil);
                    return;
                }

                this.playerAuction.setCreateEconomy(economy);
                Utils.sendMessage(player, "selected_economy", placeholderUtil);
            }));
        }
    }
}
