package me.sedattr.deluxeauctions.menus;

import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class BinViewMenu {
    private final Player player;
    private final Auction auction;
    private final ConfigurationSection section;
    private HInventory gui;
    private BukkitTask itemUpdater;
    private String back;

    public BinViewMenu(Player player, Auction auction) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("bin_auction_view_menu");
        this.auction = auction;
    }

    public void open(String back) {
        if (auction.isEnded()) {
            PlayerBid playerBid = this.auction.getAuctionBids().getPlayerBid(this.player.getUniqueId());

            PlaceholderUtil placeholderUtil = new PlaceholderUtil();
            if (auction.getAuctionOwner().equals(player.getUniqueId()) && auction.isSellerClaimed()) {
                Utils.sendMessage(player, "ended_auction", placeholderUtil
                        .addPlaceholder("%item_displayname%", Utils.getDisplayName(auction.getAuctionItem())));
                return;
            } else if (!auction.getAuctionOwner().equals(player.getUniqueId()))
                if (playerBid == null || playerBid.isCollected()) {
                    Utils.sendMessage(player, "ended_auction", placeholderUtil
                            .addPlaceholder("%item_displayname%", Utils.getDisplayName(auction.getAuctionItem())));
                    return;
                }
        }

        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "view", new PlaceholderUtil()
                .addPlaceholder("%auction_type%", auction.getAuctionType().name()));

        this.back = back;
        if (!this.back.equals("command")) {
            int goBackSlot = this.section.getInt("back");
            ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
            if (goBackSlot > 0 && goBackItem != null)
                gui.setItem(goBackSlot-1, ClickableItem.of(goBackItem, (event) -> goBack()));
        }

        loadExampleItem();
        if (this.player.getUniqueId().equals(this.auction.getAuctionOwner()))
            loadSellerItem();
        else
            loadPurchaseItem();

        this.gui.open(this.player);
        updateExampleItem();
    }

    private void goBack() {
        if (this.back.equalsIgnoreCase("command"))
            return;

        PlayerPreferences playerAuction = PlayerCache.getPreferences(this.player.getUniqueId());
        switch (this.back) {
            case "bids" -> new BidsMenu(this.player).open(1);
            case "manage" -> new ManageMenu(this.player).open(1);
            case "auctions" -> new AuctionsMenu(this.player).open(auction.getAuctionCategory(), playerAuction.getPage());
            default -> {
                if (!this.back.isEmpty()) {
                    UUID uuid = UUID.fromString(this.back);
                    new ViewAuctionsMenu(this.player, Bukkit.getOfflinePlayer(uuid)).open(1);
                } else {
                    new AuctionsMenu(this.player).open(auction.getAuctionCategory(), playerAuction.getPage());
                }
            }
        }
    }

    private void loadSellerItem() {
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%item_displayname%", Utils.getDisplayName(this.auction.getAuctionItem()))
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(this.auction.getAuctionPrice()));

        List<PlayerBid> auctionBids = this.auction.getAuctionBids().getPlayerBids();
        if (this.auction.isEnded()) {
            ConfigurationSection collectSection = this.section.getConfigurationSection("collect_auction");
            if (collectSection == null)
                return;

            ItemStack itemStack;
            if (auctionBids.isEmpty()) {
                collectSection = collectSection.getConfigurationSection("not_sold");

                itemStack = Utils.createItemFromSection(collectSection, null);
            }
            else {
                collectSection = collectSection.getConfigurationSection("sold");
                PlayerBid playerBid = this.auction.getAuctionBids().getHighestBid();

                placeholderUtil
                        .addPlaceholder("%buyer_displayname%", playerBid.getBidOwnerDisplayName());

                itemStack = Utils.createItemFromSection(collectSection, placeholderUtil);
            }
            if (itemStack == null)
                return;

            this.gui.setItem(collectSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
                this.player.closeInventory();

                String type = this.auction.sellerCollect(this.player, false);
                if (type.isEmpty())
                    return;

                Utils.playSound(player, "seller_collected_auction");

                if (type.equals("item"))
                    Utils.sendMessage(this.player, "seller_collected_item", placeholderUtil);
                else if (type.equals("money"))
                    Utils.sendMessage(this.player, "seller_collected_money", placeholderUtil);

                goBack();
            }));
        } else {
            ConfigurationSection cancelSection = this.section.getConfigurationSection("cancel_auction");
            if (cancelSection == null)
                return;

            ItemStack itemStack = Utils.createItemFromSection(cancelSection, null);
            if (itemStack == null)
                return;

            this.gui.setItem(cancelSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
                this.player.closeInventory();

                if (this.auction.cancel(this.player)) {
                    Utils.playSound(player, "cancel_auction");

                    placeholderUtil
                            .addPlaceholder("%auction_type%", this.auction.getAuctionType().name())
                            .addPlaceholder("%item_name%", Utils.strip(Utils.getDisplayName(this.auction.getAuctionItem())))
                            .addPlaceholder("%player_name%", this.player.getName());

                    Utils.sendMessage(this.player, "cancelled", placeholderUtil);
                    if (DeluxeAuctions.getInstance().discordWebhook != null)
                        DeluxeAuctions.getInstance().discordWebhook.sendMessage("cancel_auction", placeholderUtil);
                }

                goBack();
            }));
        }
    }

    private void loadPurchaseItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("purchase_item." + (DeluxeAuctions.getInstance().economyManager.getBalance(this.player) >= this.auction.getAuctionPrice() ? "enough_money" : "not_enough_money"));
        if (itemSection == null)
            return;

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(this.auction.getAuctionPrice()));
        ItemStack itemStack = Utils.createItemFromSection(itemSection, placeholderUtil);

        if (DeluxeAuctions.getInstance().economyManager.getBalance(this.player) >= this.auction.getAuctionPrice())
            this.gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> new ConfirmMenu(this.player, "confirm_purchase").setAuction(this.auction).open()));
        else
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> Utils.sendMessage(this.player, "not_enough_money", new PlaceholderUtil().addPlaceholder("%required_money%", DeluxeAuctions.getInstance().numberFormat.format(this.auction.getAuctionPrice()-DeluxeAuctions.getInstance().economyManager.getBalance(this.player))))));
    }

    private void loadExampleItem() {
        ItemStack itemStack = AuctionHook.getUpdatedAuctionItem(this.auction);
        if (itemStack == null)
            return;

        gui.setItem(this.section.getInt("example_item")-1, ClickableItem.of(itemStack, (event) -> {
            Utils.broadcastMessage(this.player, "auction_view_info", new PlaceholderUtil()
                    .addPlaceholder("%auction_uuid%", String.valueOf(this.auction.getAuctionUUID())));

            if (itemStack.getType().toString().endsWith("SHULKER_BOX"))
                new ShulkerViewMenu(this.player, this.auction).open();
        }));
    }

    private void updateExampleItem() {
        if (this.itemUpdater != null)
            this.itemUpdater.cancel();

        this.itemUpdater = Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), () -> {
            HInventory inventory = InventoryAPI.getInventory(this.player);
            if (inventory == null || !inventory.getId().equalsIgnoreCase("view")) {
                this.itemUpdater.cancel();
                return;
            }

            loadExampleItem();
        }, 20, 20);
    }
}
