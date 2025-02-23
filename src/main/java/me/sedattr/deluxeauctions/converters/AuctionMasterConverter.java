package me.sedattr.deluxeauctions.converters;

import me.qKing12.AuctionMaster.AuctionMaster;
import me.qKing12.AuctionMaster.AuctionObjects.Auction;
import me.qKing12.AuctionMaster.AuctionObjects.Bids;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.others.Logger;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionMasterConverter {
    public CompletableFuture<Boolean> convertAuctions() {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, Auction> auctions = AuctionMaster.auctionsHandler.auctions;
            if (auctions.isEmpty()) {
                Logger.sendConsoleMessage("No auctions found in AuctionMaster plugin!", Logger.LogLevel.ERROR);
                return false;
            }

            DeluxeAuctions.getInstance().converting = true;

            AtomicInteger skipped = new AtomicInteger(0);
            AtomicInteger total = new AtomicInteger(0);
            for (Map.Entry<String, Auction> entry : auctions.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                Auction auction = entry.getValue();

                long endTime = auction.getEndingDate() / 1000;
                long daysTime = DeluxeAuctions.getInstance().configFile.getInt("settings.purge_auctions", 0) * 86400L;
                if (daysTime > 0) {
                    if (ZonedDateTime.now().toInstant().getEpochSecond() > (daysTime + endTime)) {
                        skipped.incrementAndGet();
                        continue;
                    }
                }

                double price = auction.getCoins();
                if (price <= 0) {
                    Logger.sendConsoleMessage("Auction (" + uuid + ") price is 0!", Logger.LogLevel.WARN);
                    continue;
                }

                String displayName = auction.getSellerDisplayName();
                ItemStack item = auction.getItemStack();
                if (item == null) {
                    Logger.sendConsoleMessage("Auction (" + uuid + ") item is not found!", Logger.LogLevel.WARN);
                    continue;
                }

                UUID seller = UUID.fromString(auction.getSellerUUID());
                AuctionType type = auction.isBIN() ? AuctionType.BIN : AuctionType.NORMAL;

                List<Auction> ownedAuctions = AuctionMaster.auctionsHandler.ownAuctions.getOrDefault(seller.toString(), new ArrayList<>());
                boolean sellerClaimed = !ownedAuctions.contains(auction);

                me.sedattr.deluxeauctions.managers.Auction newAuction = new me.sedattr.deluxeauctions.managers.Auction(uuid, seller, displayName, item, price, type, null, endTime, sellerClaimed);
                if (newAuction.getAuctionCategory().isEmpty()) {
                    Logger.sendConsoleMessage("Auction (" + uuid + ") category is not found!", Logger.LogLevel.WARN);
                    continue;
                }

                List<Bids.Bid> bids = auction.getBids().getBidList();
                if (!bids.isEmpty())
                    for (Bids.Bid bid : bids) {
                        PlayerBid playerBid = new PlayerBid(UUID.fromString(bid.getBidderUUID()), bid.getBidderDisplayName(), bid.getCoins(), bid.getBidDate() / 1000);
                        if (type.equals(AuctionType.BIN))
                            playerBid.setCollected(true);
                        else
                            playerBid.setCollected(bid.isClaimed());

                        newAuction.getAuctionBids().addPlayerBid(playerBid);
                    }

                AuctionCache.addAuction(newAuction);
                total.incrementAndGet();
            }

            Logger.sendConsoleMessage("&f" + total.get() + " %level_color%auctions are converted!", Logger.LogLevel.INFO);
            Logger.sendConsoleMessage("&f" + skipped.get() + " %level_color%expired auctions are not converted!", Logger.LogLevel.INFO);

            DeluxeAuctions.getInstance().databaseManager.saveAuctions();
            return true;
        });
    }
}
