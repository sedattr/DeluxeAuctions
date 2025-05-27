package me.sedattr.deluxeauctions.menus;

import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.*;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.TaskUtils;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BidsMenu {
    private final Player player;
    private final ConfigurationSection section;
    private HInventory gui;
    private boolean itemUpdater = false;
    private final PlayerPreferences playerAuction;
    private final List<Auction> bids;
    private final int totalPage;
    private int page;
    private String back;

    public BidsMenu(Player player) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("view_bids");

        this.playerAuction = PlayerCache.getPreferences(player.getUniqueId());
        this.bids = AuctionCache.getBidAuctions(player.getUniqueId());
        this.totalPage = getTotalPage();
    }

    public void open(int page, String back) {
        this.page = page;
        this.back = back;

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%current_page%", String.valueOf(page))
                .addPlaceholder("%total_page%", String.valueOf(getTotalPage()));

        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "bids", placeholderUtil);

        if (!back.equals("command")) {
            int goBackSlot = this.section.getInt("back");
            ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
            if (goBackSlot > 0 && goBackItem != null)
                gui.setItem(goBackSlot, ClickableItem.of(goBackItem, (event) -> {
                    switch (back) {
                        case "main":
                            AuctionHook.openMainMenu(this.player);
                            return;
                        case "auctions":
                            new AuctionsMenu(this.player).open(this.playerAuction.getCategory().getName(), this.playerAuction.getPage());
                            return;
                    }
                }));
        }

        loadClaimAll();
        loadBids();

        if (this.section.getBoolean("pagination")) {
            if (this.totalPage > page) {
                ItemStack nextPage = Utils.createItemFromSection(this.section.getConfigurationSection("next_page"), placeholderUtil);
                if (nextPage != null) {
                    this.gui.setItem(this.section.getInt("next_page.slot"), ClickableItem.of(nextPage, (event -> {
                        ClickType clickType = event.getClick();
                        if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                            open(this.totalPage, back);
                        else
                            open(page+1, back);
                    })));
                }
            }

            if (page > 1) {
                ItemStack previousPage = Utils.createItemFromSection(this.section.getConfigurationSection("previous_page"), placeholderUtil);
                if (previousPage != null)
                    this.gui.setItem(this.section.getInt("previous_page.slot"), ClickableItem.of(previousPage, (event -> {
                        ClickType clickType = event.getClick();
                        if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                            open(1, back);
                        else
                            open(page-1, back);
                    })));
            }
        }

        this.gui.open(this.player);
        updateItems();
    }

    private int getTotalPage() {
        List<Integer> slots = this.section.getIntegerList("slots");
        if (slots.isEmpty())
            return 1;

        int totalPage = this.bids.size() / slots.size() + 1;
        if (this.bids.size() <= slots.size()*(totalPage-1))
            totalPage--;

        return Math.max(totalPage, 1);
    }

    private void loadBids() {
        List<Integer> slots = this.section.getIntegerList("slots");
        if (slots.isEmpty())
            return;

        int lower = (this.page - 1) * slots.size();
        int upper = Math.min(this.page * slots.size(), this.bids.size());

        List<Auction> newAuctions = new ArrayList<>(upper - lower);
        for (int i = lower; i < upper; i++) {
            Auction auction = this.bids.get(i);
            newAuctions.add(auction);
        }

        int i = 0;
        for (Auction auction : newAuctions) {
            ItemStack itemStack = AuctionHook.getUpdatedAuctionItem(auction);
            if (itemStack == null)
                continue;

            int slot = i >= slots.size() ? 0 : slots.get(i);
            this.gui.setItem(slot, ClickableItem.of(itemStack, (event) -> {
                if (auction.getAuctionType().equals(AuctionType.BIN))
                    new BinViewMenu(this.player, auction).open("bids");
                else
                    new NormalViewMenu(this.player, auction).open("bids");
            }));

            i++;
        }
    }


    private void loadClaimAll() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("claim_all");
        if (itemSection == null)
            return;

        double money = 0;
        int item = 0;
        for (Auction auction : this.bids) {
            if (!auction.isEnded())
                continue;

            PlayerBid playerBid = auction.getAuctionBids().getPlayerBid(player.getUniqueId());
            if (auction.getAuctionBids().getHighestBid().equals(playerBid))
                item++;
            else
                money+=playerBid.getBidPrice();
        }

        if (money == 0.0 && item == 0) {
            ConfigurationSection claimSection = itemSection.getConfigurationSection("without_claimable");

            ItemStack itemStack = Utils.createItemFromSection(claimSection, null);
            if (itemStack == null)
                return;

            this.gui.setItem(claimSection.getInt("slot"), ClickableItem.empty(itemStack));
        } else {
            ConfigurationSection claimSection = itemSection.getConfigurationSection("with_claimable");
            PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                    .addPlaceholder("%claimable_items%", String.valueOf(item))
                    .addPlaceholder("%claimable_money%", DeluxeAuctions.getInstance().numberFormat.format(money));

            ItemStack itemStack = Utils.createItemFromSection(claimSection, placeholderUtil);
            if (itemStack == null)
                return;

            this.gui.setItem(claimSection.getInt("slot"), ClickableItem.of(itemStack, (event) -> this.playerAuction.collectBids(this.player, this.back)));
        }
    }

    private void updateItems() {
        if (this.itemUpdater)
            return;

        this.itemUpdater = true;
        Runnable runnable = this::loadBids;
        TaskUtils.runTimerAsync(this.player, "bids", runnable, 20, 20);
    }
}
