package me.sedattr.deluxeauctions.converters;

import fr.maxlego08.zauctionhouse.ZAuctionPlugin;
import fr.maxlego08.zauctionhouse.api.AuctionItem;
import fr.maxlego08.zauctionhouse.api.AuctionManager;
import fr.maxlego08.zauctionhouse.api.enums.StorageType;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.others.Logger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class ZAuctionHouseConverter {
    public CompletableFuture<Boolean> convertAuctions() {
        return CompletableFuture.supplyAsync(() -> {
            ZAuctionPlugin zAuctionPlugin = (ZAuctionPlugin) Bukkit.getPluginManager().getPlugin("zAuctionHouseV3");
            if (zAuctionPlugin == null) {
                Logger.sendConsoleMessage("zAuctionHouse plugin is not found!", Logger.LogLevel.ERROR);
                return false;
            }

            AuctionManager auctionManager = zAuctionPlugin.getAuctionManager();
            if (auctionManager == null) {
                Logger.sendConsoleMessage("No auctions found in zAuctionHouse plugin!", Logger.LogLevel.ERROR);
                return false;
            }

            List<AuctionItem> auctions = new ArrayList<>();
            auctions.addAll(auctionManager.getStorage().getItems(zAuctionPlugin, StorageType.BUY));
            auctions.addAll(auctionManager.getStorage().getItems(zAuctionPlugin, StorageType.STORAGE));
            auctions.addAll(auctionManager.getStorage().getItems(zAuctionPlugin, StorageType.EXPIRE));

            DeluxeAuctions.getInstance().converting = true;

            AtomicInteger skipped = new AtomicInteger(0);
            AtomicInteger total = new AtomicInteger(0);
            for (AuctionItem auction : auctions) {
                UUID uuid = auction.getUniqueId();

                long endTime = auction.getExpireAt()/1000;
                long daysTime = DeluxeAuctions.getInstance().configFile.getInt("settings.purge_auctions", 0) * 86400L;
                if (daysTime > 0) {
                    daysTime += daysTime + endTime;
                    if (ZonedDateTime.now().toInstant().getEpochSecond() > daysTime) {
                        skipped.incrementAndGet();
                        continue;
                    }
                }

                if (auction.getStorageType() == StorageType.EXPIRE)
                    endTime = ZonedDateTime.now().toInstant().getEpochSecond()-1000;

                double price = auction.getPrice();
                String displayName = auction.getSellerName();
                ItemStack item = auction.getItemStack();
                UUID seller = auction.getSellerUniqueId();
                AuctionType type = AuctionType.BIN;

                me.sedattr.deluxeauctions.managers.Auction newAuction = new me.sedattr.deluxeauctions.managers.Auction(uuid, seller, displayName, item, price, type, auction.getEconomyName(), endTime, false);
                if (newAuction.getAuctionCategory().isEmpty()) {
                    Logger.sendConsoleMessage("Auction (" + uuid + ") category is not found!", Logger.LogLevel.WARN);
                    continue;
                }

                if (auction.getStorageType().equals(StorageType.BUY)) {
                    UUID buyer = auction.getBuyerUniqueId();
                    if (buyer != null) {
                        newAuction.setSellerClaimed(true);
                        PlayerBid playerBid = new PlayerBid(buyer, auction.getBuyer().getName(), price, endTime);

                        newAuction.getAuctionBids().addPlayerBid(playerBid);
                    }
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