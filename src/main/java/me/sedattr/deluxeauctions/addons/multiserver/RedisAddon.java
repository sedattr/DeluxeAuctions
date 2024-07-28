package me.sedattr.deluxeauctions.addons.multiserver;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Logger;
import me.sedattr.deluxeauctionsredis.RedisManager;

import java.util.UUID;

public class RedisAddon implements MultiServerManager {

    public RedisAddon() {
        RedisManager.getInstance().initialize();
    }

    private void publish(String text) {
        DeluxeAuctions.getInstance().dataHandler.debug("SENT Redis Message: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
        RedisManager.getInstance().publish(text);
    }

    private boolean publish(UUID uuid, String text) {
        DeluxeAuctions.getInstance().dataHandler.debug("SENT Redis Message: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
        return RedisManager.getInstance().publish(String.valueOf(uuid), text);
    }

    @Override
    public void reload() {
        publish("RELOAD");
    }

    @Override
    public void updateStats(UUID playerUUID) {
        publish("STATS_UPDATE:" + playerUUID);
    }

    @Override
    public boolean loadAuction(UUID auctionUUID) {
        return publish(auctionUUID, "AUCTION_LOAD:" + auctionUUID);
    }

    @Override
    public boolean deleteAuction(UUID auctionUUID) {
        return publish(auctionUUID, "AUCTION_DELETE:" + auctionUUID);
    }

    @Override
    public boolean sellerCollectedAuction(UUID auctionUUID) {
        return publish(auctionUUID, "AUCTION_SELLER_COLLECTED:" + auctionUUID);
    }

    @Override
    public boolean buyerCollectedAuction(UUID auctionUUID, UUID playerUUID) {
        return publish(auctionUUID, "AUCTION_BUYER_COLLECTED:" + auctionUUID + ":" + playerUUID);
    }

    @Override
    public boolean playerBoughtAuction(UUID auctionUUID, UUID playerUUID) {
        return publish(auctionUUID, "AUCTION_BOUGHT:" + auctionUUID + ":" + playerUUID);
    }

    @Override
    public boolean playerPlaceBidAuction(UUID auctionUUID, UUID playerUUID, double bidPrice) {
        return publish(auctionUUID, "AUCTION_PLACE_BID:" + auctionUUID + ":" + playerUUID + ":" + bidPrice);
    }

    @Override
    public boolean isAuctionUpdating(UUID uuid) {
        return RedisManager.getInstance().isAuctionMessagePublished(String.valueOf(uuid));
    }

    @Override
    public void removeUpdatingAuction(String uuid, String text) {
        RedisManager.getInstance().removeAuctionMessage(uuid, text);
    }
}
