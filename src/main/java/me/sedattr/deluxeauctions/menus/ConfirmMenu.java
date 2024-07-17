package me.sedattr.deluxeauctions.menus;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.api.AuctionHook;
import me.sedattr.deluxeauctions.api.events.AuctionCreateEvent;
import me.sedattr.deluxeauctions.api.events.AuctionPurchaseEvent;
import me.sedattr.deluxeauctions.api.events.PlayerBidEvent;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.managers.*;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ConfirmMenu {
    private final Player player;
    private final PlayerPreferences playerAuction;
    private final ConfigurationSection section;
    private final String type;

    private HInventory gui;
    private Auction auction;
    private double price;

    public ConfirmMenu setPrice(double price) {
        this.price = price;
        return this;
    }

    public ConfirmMenu setAuction(Auction auction) {
        this.auction = auction;
        return this;
    }

    public ConfirmMenu(Player player, String type) {
        this.player = player;
        this.type = type;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection(type);
        this.playerAuction = PlayerCache.getPreferences(player.getUniqueId());
        if (this.section == null)
            return;

        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, type, null);

        int goBackSlot = this.section.getInt("back");
        ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
        if (goBackSlot > 0 && goBackItem != null)
            gui.setItem(goBackSlot-1, ClickableItem.of(goBackItem, (event) -> {
                switch (type) {
                    case "confirm_purchase" -> new BinViewMenu(this.player, this.auction).open("auctions");
                    case "confirm_auction" -> new CreateMenu(this.player).open("main");
                    case "confirm_bid" -> new NormalViewMenu(this.player, this.auction).open("auctions");
                }
            }));
    }

    public void open() {
        ConfigurationSection cancelSection = this.section.getConfigurationSection("cancel");
        ItemStack cancel = Utils.createItemFromSection(cancelSection, null);
        if (cancel != null)
            gui.setItem(cancelSection.getInt("slot")-1, ClickableItem.of(cancel, (event) -> {
                switch (type) {
                    case "confirm_purchase" -> {
                        if (auction.getAuctionType().equals(AuctionType.BIN))
                            new BinViewMenu(this.player, auction).open("auctions");
                        else
                            new NormalViewMenu(this.player, auction).open("auctions");
                    }
                    case "confirm_auction" -> player.closeInventory();
                    case "confirm_bid" -> new NormalViewMenu(this.player, auction).open("auctions");
                }
            }));

        ConfigurationSection confirmSection = this.section.getConfigurationSection("confirm");

        PlaceholderUtil placeholderUtil = new PlaceholderUtil();
        switch (type) {
            case "confirm_auction" -> {
                ItemStack createItem = PlayerCache.getItem(this.player.getUniqueId());
                if (this.section.getInt("example_item") > 0)
                    this.gui.setItem(this.section.getInt("example_item")-1, ClickableItem.empty(createItem.clone()));

                placeholderUtil
                        .addPlaceholder("%auction_fee%", DeluxeAuctions.getInstance().numberFormat.format(this.price))
                        .addPlaceholder("%item_displayname%", Utils.getDisplayName(createItem));
            }
            case "confirm_purchase" -> {
                if (this.section.getInt("example_item") > 0)
                    this.gui.setItem(this.section.getInt("example_item")-1, ClickableItem.empty(auction.getAuctionItem().clone()));

                placeholderUtil
                        .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(auction.getAuctionPrice()))
                        .addPlaceholder("%item_displayname%", Utils.getDisplayName(auction.getAuctionItem()));
            }
            case "confirm_bid" -> {
                if (this.section.getInt("example_item") > 0)
                    this.gui.setItem(this.section.getInt("example_item")-1, ClickableItem.empty(auction.getAuctionItem().clone()));

                placeholderUtil
                        .addPlaceholder("%bid_price%", DeluxeAuctions.getInstance().numberFormat.format(this.price))
                        .addPlaceholder("%item_displayname%", Utils.getDisplayName(auction.getAuctionItem()));
            }
        }
        ItemStack confirm = Utils.createItemFromSection(confirmSection, placeholderUtil);

        if (confirm != null)
            gui.setItem(confirmSection.getInt("slot")-1, ClickableItem.of(confirm, (event) -> {
                ItemStack createItem = PlayerCache.getItem(this.player.getUniqueId());
                placeholderUtil
                        .addPlaceholder("%auction_type%", playerAuction.getCreateType().name())
                        .addPlaceholder("%player_name%", this.player.getName())
                        .addPlaceholder("%item_name%", Utils.strip(Utils.getDisplayName(this.auction != null ? this.auction.getAuctionItem() : createItem)))
                        .addPlaceholder("%buyer_displayname%", this.player.getDisplayName())
                        .addPlaceholder("%buyer_name%", this.player.getName());

                switch (type) {
                    case "confirm_auction" -> {
                        if (AuctionCache.getOwnedAuctions(this.player.getUniqueId()).size() >= AuctionHook.getLimit(this.player, "auction_limit")) {
                            Utils.sendMessage(this.player, "reached_auction_limit");
                            return;
                        }

                        AuctionType createType = playerAuction.getCreateType();
                        Auction newAuction = new Auction(createItem, playerAuction.getCreatePrice(), createType, playerAuction.getCreateTime());
                        if (newAuction.create(this.player, this.price)) {
                            Utils.sendMessage(this.player, "created_" + playerAuction.getCreateType().name().toLowerCase() + "_auction", placeholderUtil);
                            Utils.broadcastMessage(this.player, playerAuction.getCreateType().name().toLowerCase() + "_auction_broadcast", placeholderUtil
                                    .addPlaceholder("%player_displayname%", this.player.getDisplayName())
                                    .addPlaceholder("%auction_uuid%", String.valueOf(newAuction.getAuctionUUID())));

                            if (DeluxeAuctions.getInstance().discordWebhook != null)
                                DeluxeAuctions.getInstance().discordWebhook.sendMessage("create_auction", placeholderUtil);

                            if (createType == AuctionType.BIN)
                                new BinViewMenu(player, newAuction).open("manage");
                            else
                                new NormalViewMenu(player, newAuction).open("manage");
                        }
                    }
                    case "confirm_purchase" -> {
                        this.player.closeInventory();

                        boolean status = this.auction.purchase(this.player);
                        if (status) {
                            Utils.playSound(this.player, "bought_auction");
                            Utils.sendMessage(this.player, "bought", placeholderUtil);

                            Player seller = Bukkit.getPlayer(this.auction.getAuctionOwner());
                            placeholderUtil
                                    .addPlaceholder("%seller_name%", seller != null ? seller.getName() : "?");

                            if (seller != null && seller.isOnline()) {
                                Utils.playSound(seller, "sold_auction");
                                Utils.broadcastMessage(seller, "sold", placeholderUtil
                                        .addPlaceholder("%auction_uuid%", String.valueOf(this.auction.getAuctionUUID())));
                            }

                            if (DeluxeAuctions.getInstance().discordWebhook != null)
                                DeluxeAuctions.getInstance().discordWebhook.sendMessage("bought_item", placeholderUtil);

                            if (DeluxeAuctions.getInstance().discordWebhook != null)
                                DeluxeAuctions.getInstance().discordWebhook.sendMessage("sold_item", placeholderUtil);
                        }
                    }
                    case "confirm_bid" -> {
                        if (AuctionCache.getBidAuctions(this.player.getUniqueId()).size() >= AuctionHook.getLimit(this.player, "bid_limit")) {
                            Utils.sendMessage(this.player, "reached_bid_limit");
                            return;
                        }

                        boolean status = this.auction.placeBid(this.player, this.price);
                        if (status) {
                            Utils.playSound(this.player, "bid_auction");

                            placeholderUtil
                                    .addPlaceholder("%bidder_displayname%", this.player.getDisplayName())
                                    .addPlaceholder("%auction_uuid%", String.valueOf(this.auction.getAuctionUUID()));

                            for (PlayerBid playerBid : this.auction.getAuctionBids().getHighestPlayerBids()) {
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerBid.getBidOwner());
                                if (!offlinePlayer.isOnline())
                                    continue;
                                if (offlinePlayer.getUniqueId().equals(this.player.getUniqueId()))
                                    continue;

                                Utils.broadcastMessage(offlinePlayer.getPlayer(), "outbid", placeholderUtil
                                        .addPlaceholder("%outbid_price%", DeluxeAuctions.getInstance().numberFormat.format(this.price-playerBid.getBidPrice())));
                            }

                            Player seller = Bukkit.getPlayer(this.auction.getAuctionOwner());
                            placeholderUtil
                                    .addPlaceholder("%seller_name%", seller != null ? seller.getName() : "?");

                            if (DeluxeAuctions.getInstance().discordWebhook != null)
                                DeluxeAuctions.getInstance().discordWebhook.sendMessage("bid_item", placeholderUtil);
                            Utils.sendMessage(this.player, "bid", placeholderUtil);

                            if (seller != null && seller.isOnline())
                                Utils.broadcastMessage(seller, "new_bid", placeholderUtil);

                            if (this.auction.getAuctionType() == AuctionType.BIN)
                                new BinViewMenu(player, this.auction).open("bids");
                            else
                                new NormalViewMenu(player, this.auction).open("bids");
                        }
                    }
                }
            }));

        this.gui.open(this.player);
    }
}