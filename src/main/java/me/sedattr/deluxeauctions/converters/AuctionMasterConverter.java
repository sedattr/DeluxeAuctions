package me.sedattr.deluxeauctions.converters;

import me.qKing12.AuctionMaster.AuctionMaster;
import me.qKing12.AuctionMaster.AuctionObjects.Auction;
import me.qKing12.AuctionMaster.AuctionObjects.Bids;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.*;

public class AuctionMasterConverter {
    public boolean convertAuctions() {
        HashMap<String, Auction> auctions = AuctionMaster.auctionsHandler.auctions;
        if (auctions.isEmpty())
            return true;

        DeluxeAuctions.getInstance().converting = true;
        for (Map.Entry<String, Auction> entry : auctions.entrySet()) {
            UUID uuid = UUID.fromString(entry.getKey());
            Auction auction = entry.getValue();

            long endTime = auction.getEndingDate()/1000;
            long daysTime = DeluxeAuctions.getInstance().configFile.getInt("settings.purge_auctions", 0) * 86400L;
            if (daysTime > 0) {
                if (ZonedDateTime.now().toInstant().getEpochSecond() > (daysTime + endTime))
                    continue;
            }

            double price = auction.getCoins();
            if (price <= 0)
                continue;

            String displayName = auction.getSellerDisplayName();
            ItemStack item = auction.getItemStack();
            if (item == null)
                continue;

            UUID seller = UUID.fromString(auction.getSellerUUID());
            AuctionType type = auction.isBIN() ? AuctionType.BIN : AuctionType.NORMAL;

            List<Auction> ownedAuctions = AuctionMaster.auctionsHandler.ownAuctions.getOrDefault(seller.toString(), new ArrayList<>());
            boolean sellerClaimed = !ownedAuctions.contains(auction);

            me.sedattr.deluxeauctions.managers.Auction newAuction = new me.sedattr.deluxeauctions.managers.Auction(uuid, seller, displayName, item, price, type, endTime, sellerClaimed);
            if (newAuction.getAuctionCategory().isEmpty())
                continue;

            List<Bids.Bid> bids = auction.getBids().getBidList();
            if (!bids.isEmpty())
                for (Bids.Bid bid : bids) {
                    PlayerBid playerBid = new PlayerBid(UUID.fromString(bid.getBidderUUID()), bid.getBidderDisplayName(), bid.getCoins(), bid.getBidDate()/1000);
                    playerBid.setCollected(bid.isClaimed());

                    newAuction.getAuctionBids().addPlayerBid(playerBid);
                }

            AuctionCache.addAuction(newAuction);
        }

        DeluxeAuctions.getInstance().databaseManager.saveAuctions();
        return true;
    }
}
