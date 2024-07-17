package me.sedattr.deluxeauctions.menus;

import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;

public class ShulkerViewMenu {
    private final Player player;
    private final ConfigurationSection section;
    private final Auction auction;
    private HInventory gui;

    public ShulkerViewMenu(Player player, Auction auction) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("shulker_view_menu");
        this.auction = auction;
    }

    public void open() {
        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "shulker_view", null);

        int goBackSlot = this.section.getInt("back");
        ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
        if (goBackSlot > 0 && goBackItem != null)
            gui.setItem(goBackSlot-1, ClickableItem.of(goBackItem, (event) -> {
                if (auction.getAuctionType().equals(AuctionType.BIN))
                    new BinViewMenu(this.player, auction).open("auctions");
                else
                    new NormalViewMenu(this.player, auction).open("auctions");
            }));

        loadItems();
        this.gui.open(this.player);
    }

    private void loadItems() {
        BlockStateMeta bsm = (BlockStateMeta) auction.getAuctionItem().getItemMeta();
        if (bsm == null)
            return;

        ShulkerBox shulkerBox = (ShulkerBox) bsm.getBlockState();

        List<Integer> slots = this.section.getIntegerList("slots");
        ItemStack[] items = shulkerBox.getInventory().getContents();

        int i = 0;
        for (ItemStack item : items) {
            this.gui.setItem(slots.get(i)-1, ClickableItem.empty(item));

            i++;
        }
    }
}
