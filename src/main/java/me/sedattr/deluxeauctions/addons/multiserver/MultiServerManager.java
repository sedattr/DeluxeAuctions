package me.sedattr.deluxeauctions.addons.multiserver;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Logger;

import java.util.UUID;

public interface MultiServerManager {
    // Methods
    void reload(); // Reload the plugin
    void updateStats(UUID playerUUID); // Update player's all stats
    void loadAuction(UUID auctionUUID); // Load new created auction
    boolean deleteAuction(UUID auctionUUID); // Delete auction
    boolean sellerCollectedAuction(UUID auctionUUID); // Seller claimed auction (NORMAL AUCTION)
    boolean buyerCollectedAuction(UUID auctionUUID, UUID playerUUID); // Buyer collected auction (NORMAL AUCTION)
    boolean playerBoughtAuction(UUID auctionUUID, UUID playerUUID); // Player bought auction (BIN AUCTION)
    boolean playerPlaceBidAuction(UUID auctionUUID, UUID playerUUID, double bidPrice); // Player bid auction (NORMAL AUCTION)

    boolean isAuctionUpdating(UUID uuid);
    void removeUpdatingAuction(String uuid, String text);

    default void handleMessage(String text) {
        if (text == null || text.isEmpty())
            return;

        String[] args = text.split(":", 2);
        if (args.length < 1)
            return;

        String type = args[0];
        MessageType messageType = MessageType.valueOf(type);

        DeluxeAuctions.getInstance().dataHandler.debug("Handling Message: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.WARN);
        messageType.getMessage(args.length > 1 ? args[1] : "");
    }
}