package me.sedattr.deluxeauctions.managers;

import lombok.Getter;
import lombok.Setter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.api.events.*;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.cache.CategoryCache;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
public class Auction {
    private final UUID auctionUUID;
    private final double auctionPrice;
    private final ItemStack auctionItem;
    private final String auctionCategory;
    private final AuctionType auctionType;
    private final AuctionBids auctionBids = new AuctionBids();
    @Setter private long auctionEndTime;
    private UUID auctionOwner;
    private String auctionOwnerDisplayName;
    @Setter private boolean sellerClaimed = false;

    public Auction(ItemStack item, Double price, AuctionType type, long time) {
        this.auctionUUID = UUID.randomUUID();
        this.auctionPrice = price;
        this.auctionItem = item;
        this.auctionType = type;
        this.auctionEndTime = ZonedDateTime.now().toInstant().getEpochSecond() + time;
        this.auctionCategory = CategoryCache.getItemCategory(item);
    }

    public Auction(UUID uuid, UUID owner, String displayName, ItemStack item, Double price, AuctionType type, long end, boolean sellerClaimed) {
        this.auctionUUID = uuid;
        this.auctionPrice = price;
        this.auctionItem = item;
        this.auctionType = type;
        this.auctionEndTime = end;
        this.auctionOwnerDisplayName = displayName;
        this.auctionOwner = owner;
        this.sellerClaimed = sellerClaimed;
        this.auctionCategory = CategoryCache.getItemCategory(item);
    }

    public boolean create(Player player, double totalFee) {
        if (this.auctionCategory.isEmpty())
            return false;

        double balance = DeluxeAuctions.getInstance().economyManager.getBalance(player);
        if (balance < totalFee) {
            Utils.sendMessage(player, "not_enough_money", new PlaceholderUtil()
                    .addPlaceholder("%required_money%", DeluxeAuctions.getInstance().numberFormat.format(totalFee-balance)));
            return false;
        }

        this.auctionOwnerDisplayName = !player.getDisplayName().isEmpty() ? player.getDisplayName() : player.getName();
        this.auctionOwner = player.getUniqueId();

        AuctionCreateEvent event = new AuctionCreateEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        AuctionCache.addAuction(this);
        DeluxeAuctions.getInstance().economyManager.removeBalance(player, totalFee);

        PlayerPreferences playerPreferences = PlayerCache.getPreferences(this.auctionOwner);
        playerPreferences.updateCreate(null);

        PlayerStats stats = PlayerCache.getStats(this.auctionOwner);
        stats.addCreatedAuction();
        stats.addTotalFees(totalFee);

        DeluxeAuctions.getInstance().dataHandler.writeToLog("[PLAYER CREATED AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") created " + this.auctionType + " auction for " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") auction!");
        DeluxeAuctions.getInstance().databaseManager.saveAuction(this);
        DeluxeAuctions.getInstance().databaseManager.saveStats(stats);
        return true;
    }

    public boolean isEnded() {
        if (this.sellerClaimed)
            return true;

        if (this.auctionType == AuctionType.BIN)
            if (!this.auctionBids.getPlayerBids().isEmpty())
                return true;

        return ZonedDateTime.now().toInstant().getEpochSecond() > this.auctionEndTime;
    }

    public boolean cancel(Player player) {
        if (isEnded())
            return false;

        if (this.isSellerClaimed())
            return false;

        if (!this.auctionBids.getPlayerBids().isEmpty())
            return false;

        if (!Utils.hasEmptySlot(player)) {
            Utils.sendMessage(player, "no_empty_slot");
            return false;
        }

        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return false;
        }

        if (DeluxeAuctions.getInstance().databaseManager.isAuctionLoading(this.auctionUUID)) {
            Utils.sendMessage(player, "refreshing");
            return false;
        }

        if (DeluxeAuctions.getInstance().multiServerManager != null && DeluxeAuctions.getInstance().multiServerManager.checkAuction(this.auctionUUID.toString())) {
            Utils.sendMessage(player, "refreshing");
            return false;
        }

        AuctionCancelEvent event = new AuctionCancelEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        // Give Item
        this.sellerClaimed = true;
        player.getInventory().addItem(this.auctionItem.clone());

        // Log
        DeluxeAuctions.getInstance().dataHandler.writeToLog("[PLAYER CANCELLED AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") cancelled " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") auction!");

        // Stats
        PlayerStats stats = PlayerCache.getStats(player.getUniqueId());
        stats.removeCreatedAuction();

        // Remove from Variables
        AuctionCache.removeAuction(this.auctionUUID);

        // Database
        DeluxeAuctions.getInstance().databaseManager.deleteAuction(this.auctionUUID.toString());
        DeluxeAuctions.getInstance().databaseManager.saveStats(stats);
        return true;
    }

    public boolean placeBid(Player player, double price) {
        if (isEnded())
            return false;
        if (this.isSellerClaimed())
            return false;
        if (this.auctionType != AuctionType.NORMAL)
            return false;
        if (DeluxeAuctions.getInstance().economyManager.getBalance(player) < price)
            return false;
        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return false;
        }

        if (DeluxeAuctions.getInstance().databaseManager.isAuctionLoading(this.auctionUUID)) {
            Utils.sendMessage(player, "refreshing");
            return false;
        }

        if (DeluxeAuctions.getInstance().multiServerManager != null && DeluxeAuctions.getInstance().multiServerManager.checkAuction(this.auctionUUID.toString())) {
            Utils.sendMessage(player, "refreshing");
            return false;
        }

        PlayerBidEvent event = new PlayerBidEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        // Add Time
        this.auctionEndTime += DeluxeAuctions.getInstance().configFile.getLong("settings.add_time_when_bid", 0);

        // Log
        DeluxeAuctions.getInstance().dataHandler.writeToLog("[PLAYER BID AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") bid " + price + " COINS for " + Bukkit.getOfflinePlayer(this.auctionOwner).getName() + "'s " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ")!");

        // Balance
        DeluxeAuctions.getInstance().economyManager.removeBalance(player, price);

        // Highest Bid
        PlayerBid highestBid = this.auctionBids.getHighestBid();
        if (highestBid != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(highestBid.getBidOwner());
            DeluxeAuctions.getInstance().economyManager.addBalance(offlinePlayer, highestBid.getBidPrice());
            highestBid.setCollected(true);
        }

        // Stats
        PlayerStats stats = PlayerCache.getStats(player.getUniqueId());
        stats.addTotalBids();
        stats.setHighestBid(price);
        stats.addSpentMoney(price);

        // Bid Auctions
        this.auctionBids.addPlayerBid(new PlayerBid(player, price));

        // Database
        DeluxeAuctions.getInstance().databaseManager.saveAuction(this);
        DeluxeAuctions.getInstance().databaseManager.saveStats(stats);

        return true;
    }

    public boolean purchase(Player player) {
        // Checking auction status
        if (isEnded())
            return false;

        // If it's claimed already, don't let to sell auction again
        if (this.isSellerClaimed())
            return false;

        // Checking auction type anyway
        if (this.auctionType != AuctionType.BIN)
            return false;

        // Balance check
        if (DeluxeAuctions.getInstance().economyManager.getBalance(player) < this.auctionPrice)
            return false;

        // Empty slot check
        if (!Utils.hasEmptySlot(player)) {
            Utils.sendMessage(player, "no_empty_slot");
            return false;
        }

        // Lag check
        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return false;
        }

        // Auction status in database
        if (DeluxeAuctions.getInstance().databaseManager.isAuctionLoading(this.auctionUUID)) {
            Utils.sendMessage(player, "refreshing");
            return false;
        }

        if (DeluxeAuctions.getInstance().multiServerManager != null && DeluxeAuctions.getInstance().multiServerManager.checkAuction(this.auctionUUID.toString())) {
            Utils.sendMessage(player, "refreshing");
            return false;
        }

        // Cancellable event
        AuctionPurchaseEvent event = new AuctionPurchaseEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        // Player
        DeluxeAuctions.getInstance().economyManager.removeBalance(player, this.auctionPrice);
        player.getInventory().addItem(this.auctionItem.clone());

        // Log
        DeluxeAuctions.getInstance().dataHandler.writeToLog("[PLAYER BOUGHT AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") bought " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") for " + this.auctionPrice + " COINS from " + Bukkit.getOfflinePlayer(this.auctionOwner).getName() + "!");

        // Bid auctions
        PlayerBid playerBid = new PlayerBid(player, this.auctionPrice, true);
        this.auctionBids.addPlayerBid(playerBid);

        // Stats
        PlayerStats stats = PlayerCache.getStats(player.getUniqueId());
        stats.addSpentMoney(this.auctionPrice);
        stats.addWonAuction();

       // Database
        DeluxeAuctions.getInstance().databaseManager.saveAuction(this);
        DeluxeAuctions.getInstance().databaseManager.saveStats(stats);

        return true;
    }

    public String sellerCollect(Player player) {
        if (!isEnded())
            return "";
        if (this.isSellerClaimed())
            return "";
        PlayerBid highestBid = this.auctionBids.getHighestBid();
        if (highestBid == null && !Utils.hasEmptySlot(player)) {
            Utils.sendMessage(player, "no_empty_slot");
            return "";
        }

        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return "";
        }

        if (DeluxeAuctions.getInstance().databaseManager.isAuctionLoading(this.auctionUUID)) {
            Utils.sendMessage(player, "refreshing");
            return "";
        }

        if (DeluxeAuctions.getInstance().multiServerManager != null && DeluxeAuctions.getInstance().multiServerManager.checkAuction(this.auctionUUID.toString())) {
            Utils.sendMessage(player, "refreshing");
            return "";
        }

        AuctionCollectEvent event = new AuctionCollectEvent(player, this, highestBid == null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return "";

        this.sellerClaimed = true;
        PlayerStats stats = PlayerCache.getStats(player.getUniqueId());
        String type;

        if (highestBid == null) {
            DeluxeAuctions.getInstance().dataHandler.writeToLog("[SELLER COLLECTED AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") collected ITEM from " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") auction!");
            player.getInventory().addItem(this.auctionItem.clone());
            type = "item";

            stats.addExpiredAuction();
        } else {
            DeluxeAuctions.getInstance().dataHandler.writeToLog("[SELLER COLLECTED AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") collected " + highestBid.getBidPrice() + " COINS from " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") auction!");
            DeluxeAuctions.getInstance().economyManager.addBalance(player, highestBid.getBidPrice());
            type = "money";

            stats.addSoldAuction();
            stats.addEarnedMoney(highestBid.getBidPrice());
        }
        DeluxeAuctions.getInstance().databaseManager.saveStats(stats);

        // Database
        if (this.getAuctionBids().isAllCollected()) {
            AuctionCache.removeAuction(this.auctionUUID);
            DeluxeAuctions.getInstance().databaseManager.deleteAuction(this.auctionUUID.toString());
        } else
            DeluxeAuctions.getInstance().databaseManager.saveAuction(this);
        return type;
    }

    public String buyerCollect(Player player) {
        if (!isEnded())
            return "";
        PlayerBid playerBid = this.auctionBids.getPlayerBid(player.getUniqueId());
        if (playerBid == null)
            return "";

        PlayerBid highestBid = this.auctionBids.getHighestBid();
        if (highestBid == null)
            return "";

        if (playerBid == highestBid && !Utils.hasEmptySlot(player)) {
            Utils.sendMessage(player, "no_empty_slot");
            return "";
        }

        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return "";
        }

        if (DeluxeAuctions.getInstance().databaseManager.isAuctionLoading(this.auctionUUID)) {
            Utils.sendMessage(player, "refreshing");
            return "";
        }

        if (DeluxeAuctions.getInstance().multiServerManager != null && DeluxeAuctions.getInstance().multiServerManager.checkAuction(this.auctionUUID.toString())) {
            Utils.sendMessage(player, "refreshing");
            return "";
        }

        AuctionCollectEvent event = new AuctionCollectEvent(player, this, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return "";

        String type;
        PlayerStats stats = PlayerCache.getStats(player.getUniqueId());
        if (playerBid == highestBid) {
            type = "item";
            DeluxeAuctions.getInstance().dataHandler.writeToLog("[BUYER COLLECTED AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") collected ITEM from " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") auction!");
            player.getInventory().addItem(this.auctionItem.clone());

            stats.addWonAuction();
        } else {
            type = "money";
            DeluxeAuctions.getInstance().dataHandler.writeToLog("[BUYER COLLECTED AUCTION] " + player.getName() + " (" + player.getUniqueId() + ") collected from " + Utils.getDisplayName(this.auctionItem) + " (" + this.auctionUUID + ") auction!");

            if (!playerBid.isCollected()) {
                DeluxeAuctions.getInstance().economyManager.addBalance(player, playerBid.getBidPrice());
                playerBid.setCollected(true);
            }

            stats.addLostAuction();
        }

        DeluxeAuctions.getInstance().databaseManager.saveStats(stats);

        // Database
        if (this.sellerClaimed && this.getAuctionBids().isAllCollected()) {
            AuctionCache.removeAuction(this.auctionUUID);
            DeluxeAuctions.getInstance().databaseManager.deleteAuction(this.auctionUUID.toString());
        } else
            DeluxeAuctions.getInstance().databaseManager.saveAuction(this);
        return type;
    }
}
