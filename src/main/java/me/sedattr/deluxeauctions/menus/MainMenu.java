package me.sedattr.deluxeauctions.menus;

import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MainMenu {
    private final Player player;
    private final ConfigurationSection section;
    private HInventory gui;
    private final PlayerPreferences playerAuction;

    public MainMenu(Player player) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("main_menu");
        this.playerAuction = PlayerCache.getPreferences(this.player.getUniqueId());
    }

    public void open() {
        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "main", null);

        loadAuctionsBrowserItem();
        loadStatsItem();
        loadCreateAuctionItem();
        loadBidsItem();
        loadManageItem();

        this.gui.open(this.player);
    }

    private void loadStatsItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("stats");
        if (itemSection == null)
            return;

        ItemStack item = Utils.createItemFromSection(itemSection, null);
        if (item == null)
            return;

        this.gui.setItem(itemSection, ClickableItem.of(item, (event) -> new StatsMenu(this.player).open()));
    }

    private void loadManageItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("manage");
        if (itemSection == null)
            return;

        ItemStack item = Utils.createItemFromSection(itemSection, null);
        if (item == null)
            return;

        List<Auction> ownedAuctions = AuctionCache.getOwnedAuctions(this.player.getUniqueId());
        String type;

        int endedAmount = 0;
        int itemAmount = 0;
        double moneyAmount = 0.0;
        for (Auction auction : ownedAuctions) {
            if (!auction.isEnded())
                continue;

            endedAmount++;
            PlayerBid playerBid = auction.getAuctionBids().getHighestBid();
            if (playerBid == null)
                itemAmount++;
            else
                moneyAmount+=playerBid.getBidPrice();
        }
        if (ownedAuctions.isEmpty())
            type = "without_auctions";
        else {
            if (itemAmount > 0 || moneyAmount > 0)
                type = "collectable";
            else
                type = "with_auctions";
        }

        List<String> lore = itemSection.getStringList("lore." + type);
        if (!lore.isEmpty()) {
            PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                    .addPlaceholder("%total_auction_amount%", String.valueOf(ownedAuctions.size()))
                    .addPlaceholder("%collectable_money_amount%", DeluxeAuctions.getInstance().numberFormat.format(moneyAmount))
                    .addPlaceholder("%collectable_item_amount%", String.valueOf(itemAmount))
                    .addPlaceholder("%ended_auction_amount%", String.valueOf(endedAmount));

            Utils.changeLore(item, lore, placeholderUtil);
        }

        this.gui.setItem(itemSection, ClickableItem.of(item, (event) -> new ManageMenu(this.player).open(1, "main")));
    }

    private void loadBidsItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("bids");
        if (itemSection == null)
            return;

        ItemStack item = Utils.createItemFromSection(itemSection, null);
        if (item == null)
            return;

        List<Auction> bidAuctions = AuctionCache.getBidAuctions(this.player.getUniqueId());
        String type;

        int collectableAmount = 0;
        int topBidAmount = 0;
        int itemAmount = 0;
        double moneyAmount = 0.0;
        for (Auction auction : bidAuctions) {
            PlayerBid playerBid = auction.getAuctionBids().getHighestBid();
            if (playerBid == null)
                continue;

            if (auction.isEnded()) {
                if (playerBid.getBidOwner().equals(this.player.getUniqueId()))
                    itemAmount++;
                else
                    moneyAmount+=auction.getAuctionBids().getPlayerBid(this.player.getUniqueId()).getBidPrice();

                collectableAmount++;
                continue;
            }

            if (playerBid.getBidOwner().equals(this.player.getUniqueId()))
                topBidAmount++;
        }
        int totalBidAmount = bidAuctions.size() - collectableAmount;

        if (bidAuctions.isEmpty())
            type = "no_bids";
        else {
            if (itemAmount > 0 || moneyAmount > 0) {
                if (totalBidAmount > 0)
                    type = "collectable_with_bids";
                else
                    type = "collectable_without_bids";
            } else
                type = "bids";
        }

        List<String> lore = itemSection.getStringList("lore." + type);
        if (!lore.isEmpty()) {
            PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                    .addPlaceholder("%collectable_money_amount%", DeluxeAuctions.getInstance().numberFormat.format(moneyAmount))
                    .addPlaceholder("%collectable_item_amount%", String.valueOf(itemAmount))
                    .addPlaceholder("%total_bid_amount%", String.valueOf(totalBidAmount))
                    .addPlaceholder("%top_bid_amount%", String.valueOf(topBidAmount));

            Utils.changeLore(item, lore, placeholderUtil);
        }

        this.gui.setItem(itemSection, ClickableItem.of(item, (event) -> new BidsMenu(this.player).open(1, "main")));
    }

    private void loadCreateAuctionItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("create");
        if (itemSection == null)
            return;

        ItemStack item = Utils.createItemFromSection(itemSection, null);
        if (item == null)
            return;

        this.gui.setItem(itemSection, ClickableItem.of(item, (event) -> new CreateMenu(this.player).open("main")));
    }

    private void loadAuctionsBrowserItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("browser");
        if (itemSection == null)
            return;

        ItemStack item = Utils.createItemFromSection(itemSection, null);
        if (item == null)
            return;

        this.gui.setItem(itemSection, ClickableItem.of(item, (event) -> new AuctionsMenu(this.player).open(this.playerAuction.getCategory().getName(), 1)));
    }
}
