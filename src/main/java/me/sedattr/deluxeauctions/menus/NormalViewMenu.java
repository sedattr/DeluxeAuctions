package me.sedattr.deluxeauctions.menus;

import me.sedattr.deluxeauctions.api.AuctionHook;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionBids;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class NormalViewMenu implements MenuManager {
    private final Player player;
    private final Auction auction;
    private final ConfigurationSection section;
    private HInventory gui;
    private BukkitTask itemUpdater;
    private String back;

    public NormalViewMenu(Player player, Auction auction) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("normal_auction_view_menu");
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

        this.back = back;
        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "view", new PlaceholderUtil()
                .addPlaceholder("%auction_type%", auction.getAuctionType().name()));

        if (!back.equals("command")) {
            int goBackSlot = this.section.getInt("back");
            ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
            if (goBackSlot > 0 && goBackItem != null)
                gui.setItem(goBackSlot-1, ClickableItem.of(goBackItem, (event) -> goBack()));
        }

        if (this.player.getUniqueId().equals(this.auction.getAuctionOwner()))
            loadSellerItem();
        else
        if (!this.auction.isEnded()) {
            loadBidItem();
            loadCustomBidItem();
        } else
            loadCollectItem();

        loadBidHistoryItem();
        loadExampleItem();

        this.gui.open(this.player);

        updateExampleItem();
    }

    private void goBack() {
        if (this.back.equalsIgnoreCase("command"))
            return;

        PlayerPreferences playerAuction = PlayerCache.getPreferences(player.getUniqueId());
        switch (back) {
            case "bids" -> new BidsMenu(this.player).open(1);
            case "manage" -> new ManageMenu(this.player).open(1);
            case "auctions" -> new AuctionsMenu(this.player).open(auction.getAuctionCategory(), playerAuction.getPage());
            default -> {
                if (!back.isEmpty()) {
                    UUID uuid = UUID.fromString(back);
                    new ViewAuctionsMenu(this.player, Bukkit.getOfflinePlayer(uuid)).open(1);
                } else {
                    new AuctionsMenu(this.player).open(auction.getAuctionCategory(), playerAuction.getPage());
                }
            }
        }
    }

    private void loadCollectItem() {
        PlayerBid playerBid = this.auction.getAuctionBids().getPlayerBid(this.player.getUniqueId());
        PlayerBid highestBid = this.auction.getAuctionBids().getHighestBid();

        ConfigurationSection collectSection = this.section.getConfigurationSection("buyer_collect_auction");
        if (collectSection == null)
            return;

        if (playerBid == null) {
            collectSection = collectSection.getConfigurationSection("not_bidder");

            ItemStack itemStack = Utils.createItemFromSection(collectSection, null);
            if (itemStack == null)
                return;

            this.gui.setItem(collectSection.getInt("slot")-1, ClickableItem.empty(itemStack));
            return;
        }

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%item_displayname%", Utils.getDisplayName(this.auction.getAuctionItem()))
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(playerBid.getBidPrice()))
                .addPlaceholder("%seller_displayname%", this.auction.getAuctionOwnerDisplayName());

        if (highestBid == playerBid)
            collectSection = collectSection.getConfigurationSection("top_bidder");
        else
            collectSection = collectSection.getConfigurationSection("not_top_bidder");

        ItemStack itemStack = Utils.createItemFromSection(collectSection, placeholderUtil);
        if (itemStack == null)
            return;

        this.gui.setItem(collectSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
            this.player.closeInventory();

            String type = this.auction.buyerCollect(this.player);
            if (type.isEmpty())
                return;

            Utils.playSound(player, "buyer_collected_auction");

            if (type.equals("item"))
                Utils.sendMessage(this.player, "buyer_collected_item", placeholderUtil);
            else if (type.equals("money"))
                Utils.sendMessage(this.player, "buyer_collected_money", placeholderUtil);

            goBack();
        }));
    }

    private void loadSellerItem() {
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%item_displayname%", Utils.getDisplayName(this.auction.getAuctionItem()));

        PlayerBid playerBid = this.auction.getAuctionBids().getHighestBid();
        if (this.auction.isEnded()) {
            ConfigurationSection collectSection = this.section.getConfigurationSection("collect_auction");
            if (collectSection == null)
                return;

            ItemStack itemStack;
            if (playerBid == null) {
                collectSection = collectSection.getConfigurationSection("not_sold");
                itemStack = Utils.createItemFromSection(collectSection, null);
            }
            else {
                collectSection = collectSection.getConfigurationSection("sold");
                placeholderUtil
                        .addPlaceholder("%buyer_displayname%", playerBid.getBidOwnerDisplayName())
                        .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(playerBid.getBidPrice()));

                itemStack = Utils.createItemFromSection(collectSection, placeholderUtil);
            }
            if (itemStack == null)
                return;

            this.gui.setItem(collectSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
                this.player.closeInventory();

                String type = this.auction.sellerCollect(this.player);
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

            if (playerBid == null)
                cancelSection = cancelSection.getConfigurationSection("without_bids");
            else
                cancelSection = cancelSection.getConfigurationSection("with_bids");

            ItemStack itemStack = Utils.createItemFromSection(cancelSection, null);
            if (itemStack == null)
                return;

            if (playerBid == null)
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
            else
                this.gui.setItem(cancelSection.getInt("slot")-1, ClickableItem.empty(itemStack));
        }
    }

    private double calculateBidAmount() {
        AuctionBids bids = this.auction.getAuctionBids();

        if (bids.getHighestBid() == null)
            return this.auction.getAuctionPrice();

        String bidFormula = DeluxeAuctions.getInstance().configFile.getString("settings.bid_formula", "%highest_bid% + %highest_bid% / 10");
        Expression e = new ExpressionBuilder(bidFormula
                .replace("%highest_bid%", String.valueOf(bids.getHighestBid().getBidPrice())))
                .build();
        double formulaPrice = e.evaluate();

        return Math.max(formulaPrice, bids.getHighestBid().getBidPrice());
    }

    private void loadCustomBidItem() {
        AuctionBids bids = this.auction.getAuctionBids();
        double price = calculateBidAmount();

        ConfigurationSection itemSection = this.section.getConfigurationSection("custom_bid");
        if (itemSection==null)
            return;

        if (bids.getHighestBid() != null && bids.getHighestBid().getBidOwner().equals(this.player.getUniqueId()))
            itemSection = itemSection.getConfigurationSection("top_bid");
        else {
            if (DeluxeAuctions.getInstance().economyManager.getBalance(player) >= price)
                itemSection = itemSection.getConfigurationSection("enough_money");
            else
                itemSection = itemSection.getConfigurationSection("not_enough_money");
        }
        if (itemSection==null)
            return;

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%minimum_bid_amount%", DeluxeAuctions.getInstance().numberFormat.format(price));

        ItemStack itemStack = Utils.createItemFromSection(itemSection, placeholderUtil);
        if (itemStack == null)
            return;

        if (itemSection.getName().equals("enough_money"))
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> DeluxeAuctions.getInstance().inputMenu.open(this.player, this)));
        else
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.empty(itemStack));
    }

    private void loadBidItem() {
        AuctionBids bids = this.auction.getAuctionBids();
        double price = calculateBidAmount();

        ConfigurationSection itemSection = this.section.getConfigurationSection("submit_bid");
        if (itemSection==null)
            return;

        if (bids.getHighestBid() != null && bids.getHighestBid().getBidOwner().equals(this.player.getUniqueId()))
            itemSection = itemSection.getConfigurationSection("top_bid");
        else {
            if (DeluxeAuctions.getInstance().economyManager.getBalance(player) >= price)
                itemSection = itemSection.getConfigurationSection("enough_money");
            else
                itemSection = itemSection.getConfigurationSection("not_enough_money");
        }

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(price));

        ItemStack itemStack = Utils.createItemFromSection(itemSection, placeholderUtil);
        if (itemStack == null)
            return;

        if (itemSection.getName().equals("enough_money"))
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
                double balance = DeluxeAuctions.getInstance().economyManager.getBalance(this.player);
                if (balance < price) {
                    Utils.sendMessage(this.player, "not_enough_money", placeholderUtil.addPlaceholder("%required_money%", DeluxeAuctions.getInstance().numberFormat.format(price-balance)));
                    return;
                }

                new ConfirmMenu(this.player, "confirm_bid").setAuction(this.auction).setPrice(price).open();
            }));
        else
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.empty(itemStack));
    }

    private void loadBidHistoryItem() {
        AuctionBids bids = this.auction.getAuctionBids();
        List<PlayerBid> playerBids = bids.getPlayerBids();

        ConfigurationSection itemSection = playerBids.isEmpty() ? this.section.getConfigurationSection("bid_history.without_bids") : this.section.getConfigurationSection("bid_history.with_bids");
        if (itemSection == null)
            return;

        ItemStack itemStack = Utils.createItemFromSection(itemSection, null);
        if (!playerBids.isEmpty()) {
            playerBids.sort(Comparator.comparingDouble(PlayerBid::getBidPrice).reversed());
            List<String> lore = itemSection.getStringList("lore");

            List<String> newLore = new ArrayList<>();
            for (String line : lore) {
                if (line.contains("%bid_descriptions%")) {
                    List<String> bidDescription = itemSection.getStringList("bid_description");

                    int maximum = itemSection.getInt("maximum_bids", 5);
                    int i=0;
                    for (PlayerBid bid : playerBids) {
                        if (i >= maximum)
                            break;

                        bidDescription.forEach(a -> newLore.add(Utils.colorize(a
                                .replace("%bid_time%", DeluxeAuctions.getInstance().timeFormat.formatTime(ZonedDateTime.now().toInstant().getEpochSecond()-bid.getBidTime(), "other_times"))
                                .replace("%bidder_username%", bid.getBidOwnerDisplayName())
                                .replace("%bid_amount%", DeluxeAuctions.getInstance().numberFormat.format(bid.getBidPrice())))));
                        i++;
                    }

                    continue;
                }

                newLore.add(Utils.colorize(line
                        .replace("%total_bid_amount%", String.valueOf(playerBids.size()))));
            }

            Utils.changeLore(itemStack, newLore, null);
        }

        gui.setItem(itemSection.getInt("slot")-1, ClickableItem.empty(itemStack));
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
            loadBidHistoryItem();
        }, 0, 20);
    }

    @Override
    public void inputResult(String input) {
        double number;
        try {
            number = Double.parseDouble(input);
        } catch (Exception e) {
            number = 0;
        }

        double price = calculateBidAmount();
        if (number < price) {
            open(this.back);
            return;
        }

        double balance = DeluxeAuctions.getInstance().economyManager.getBalance(this.player);

        PlayerBid oldBid = this.auction.getAuctionBids().getPlayerBid(this.player.getUniqueId());
        double money = oldBid != null ? number-oldBid.getBidPrice() : number;
        if (balance < money) {
            Utils.sendMessage(this.player, "not_enough_money", new PlaceholderUtil().addPlaceholder("%required_money%", DeluxeAuctions.getInstance().numberFormat.format(money-balance)));
            open(this.back);
            return;
        }

        new ConfirmMenu(this.player, "confirm_bid").setAuction(this.auction).setPrice(number).open();
    }
}