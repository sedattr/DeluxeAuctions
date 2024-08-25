package me.sedattr.deluxeauctions.menus;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.managers.*;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.TaskUtils;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ViewAuctionsMenu {
    private final ConfigurationSection section;
    private HInventory gui;
    private boolean itemUpdater = false;
    private List<Auction> auctions = List.of();
    private int page;
    private final Player player;
    private final OfflinePlayer target;

    public ViewAuctionsMenu(Player player, OfflinePlayer target) {
        this.player = player;
        this.target = target;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("view_player_auctions");
    }

    public void open(int page) {
        this.page = page;

        List<Auction> newAuctions = new ArrayList<>();
        List<Auction> auctions = AuctionCache.getOwnedAuctions(this.target.getUniqueId());
        for (Auction auction : auctions) {
            if (auction.isEnded())
                continue;

            newAuctions.add(auction);
        }

        if (newAuctions.isEmpty()) {
            Utils.sendMessage(this.player, "no_auction");
            return;
        }

        this.auctions = newAuctions;
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%player_name%", this.target.getName())
                .addPlaceholder("%current_page%", String.valueOf(page))
                .addPlaceholder("%total_page%", String.valueOf(getTotalPage()));

        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "view_auctions", placeholderUtil);

        loadAuctions();

        if (this.section.getBoolean("pagination")) {
            if (getTotalPage() > page) {
                ItemStack nextPage = Utils.createItemFromSection(this.section.getConfigurationSection("next_page"), placeholderUtil);
                if (nextPage != null) {
                    this.gui.setItem(this.section.getInt("next_page.slot")-1, ClickableItem.of(nextPage, (event -> {
                        ClickType clickType = event.getClick();
                        if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                            open(getTotalPage());
                        else
                            open(page+1);
                    })));
                }
            }

            if (page > 1) {
                ItemStack previousPage = Utils.createItemFromSection(this.section.getConfigurationSection("previous_page"), placeholderUtil);
                if (previousPage != null)
                    this.gui.setItem(this.section.getInt("previous_page.slot")-1, ClickableItem.of(previousPage, (event -> {
                        ClickType clickType = event.getClick();
                        if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                            open(1);
                        else
                            open(page-1);
                    })));
            }
        }

        this.gui.open(this.player);
        updateItems();
    }

    private int getTotalPage() {
        List<Integer> slots = this.section.getIntegerList("slots");

        int totalPage = this.auctions.size() / slots.size() + 1;
        if (this.auctions.size() <= slots.size()*(totalPage-1))
            totalPage--;
        if (this.auctions.isEmpty())
            totalPage=1;

        return totalPage;
    }

    private void loadAuctions() {
        List<Integer> slots = this.section.getIntegerList("slots");
        if (slots.isEmpty())
            return;

        int lower = (this.page - 1) * slots.size();
        int upper = Math.min(this.page * slots.size(), this.auctions.size());

        List<Auction> newAuctions = new ArrayList<>(upper - lower);
        for (int i = lower; i < upper; i++) {
            Auction auction = this.auctions.get(i);
            newAuctions.add(auction);
        }

        int i = 0;
        for (Auction auction : newAuctions) {
            ItemStack itemStack = AuctionHook.getUpdatedAuctionItem(auction);

            int slot = i >= slots.size() ? 0 : slots.get(i);
            this.gui.setItem(slot-1, ClickableItem.of(itemStack, (event) -> {
                if (auction.getAuctionType().equals(AuctionType.BIN))
                    new BinViewMenu(this.player, auction).open(this.target.getUniqueId().toString());
                else
                    new NormalViewMenu(this.player, auction).open(this.target.getUniqueId().toString());
            }));

            i++;
        }
    }

    private void updateItems() {
        if (this.itemUpdater)
            return;

        this.itemUpdater = true;
        Runnable runnable = this::loadAuctions;
        TaskUtils.runTimerAsync(this.player, "view_auctions", runnable, 20, 20);
    }
}
