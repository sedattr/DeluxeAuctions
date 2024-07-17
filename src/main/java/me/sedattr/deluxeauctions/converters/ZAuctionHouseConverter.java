package me.sedattr.deluxeauctions.converters;

import fr.maxlego08.zauctionhouse.ZAuctionPlugin;
import fr.maxlego08.zauctionhouse.api.AuctionItem;
import fr.maxlego08.zauctionhouse.api.AuctionManager;
import fr.maxlego08.zauctionhouse.api.enums.StorageType;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.*;

public class ZAuctionHouseConverter {
    public boolean convertAuctions() {
        ZAuctionPlugin zAuctionPlugin = (ZAuctionPlugin) Bukkit.getPluginManager().getPlugin("zAuctionHouseV3");
        if (zAuctionPlugin == null)
            return false;

        AuctionManager auctionManager = zAuctionPlugin.getAuctionManager();
        if (auctionManager == null)
            return false;

        List<AuctionItem> auctions = new ArrayList<>();
        auctions.addAll(auctionManager.getStorage().getItems(zAuctionPlugin, StorageType.BUY));
        auctions.addAll(auctionManager.getStorage().getItems(zAuctionPlugin, StorageType.STORAGE));
        auctions.addAll(auctionManager.getStorage().getItems(zAuctionPlugin, StorageType.EXPIRE));

        DeluxeAuctions.getInstance().converting = true;
        for (AuctionItem auction : auctions) {
            long endTime = auction.getExpireAt()/1000;
            long daysTime = DeluxeAuctions.getInstance().configFile.getInt("settings.purge_auctions", 0) * 86400L;
            if (daysTime > 0) {
                daysTime += daysTime + endTime;
                if (ZonedDateTime.now().toInstant().getEpochSecond() > daysTime)
                    continue;
            }

            if (auction.getStorageType() == StorageType.EXPIRE)
                endTime = ZonedDateTime.now().toInstant().getEpochSecond()-1000;

            double price = auction.getPrice();
            String displayName = auction.getSellerName();
            ItemStack item = auction.getItemStack();
            UUID seller = auction.getSellerUniqueId();
            AuctionType type = AuctionType.BIN;
            UUID uuid = auction.getUniqueId();

            me.sedattr.deluxeauctions.managers.Auction newAuction = new me.sedattr.deluxeauctions.managers.Auction(uuid, seller, displayName, item, price, type, endTime, false);
            if (newAuction.getAuctionCategory().isEmpty())
                continue;

            if (auction.getStorageType().equals(StorageType.BUY)) {
                UUID buyer = auction.getBuyerUniqueId();
                if (buyer != null) {
                    newAuction.setSellerClaimed(true);
                    PlayerBid playerBid = new PlayerBid(buyer, auction.getBuyer().getName(), price, endTime);

                    newAuction.getAuctionBids().addPlayerBid(playerBid);
                }
            }

            AuctionCache.addAuction(newAuction);
        }

        DeluxeAuctions.getInstance().databaseManager.saveAuctions();
        return true;
    }
}
