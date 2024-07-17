package me.sedattr.deluxeauctions.addons.multiserver;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.others.Logger;
import org.bukkit.Bukkit;

import java.util.UUID;

public enum MessageType {
    AUCTION_DELETE {
        @Override
        public void getMessage(String text) {
            if (text == null || text.isEmpty())
                return;

            DeluxeAuctions.getInstance().dataHandler.debug("DELETE Auction: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
            UUID uuid = UUID.fromString(text);
            Bukkit.getScheduler().runTaskLaterAsynchronously(DeluxeAuctions.getInstance(), () -> {
                AuctionCache.removeAuction(uuid);
                DeluxeAuctions.getInstance().multiServerManager.removeAuction(text);
            }, 1);
        }
    },

    AUCTION_UPDATE {
        @Override
        public void getMessage(String text) {
            if (text == null || text.isEmpty())
                return;

            DeluxeAuctions.getInstance().dataHandler.debug("UPDATE Auction: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
            UUID uuid = UUID.fromString(text);

            Bukkit.getScheduler().runTaskLater(DeluxeAuctions.getInstance(), () -> {
                DeluxeAuctions.getInstance().databaseManager.loadAuction(uuid);
                DeluxeAuctions.getInstance().multiServerManager.removeAuction(text);
            }, 1);
        }
    },

    STATS_UPDATE {
        @Override
        public void getMessage(String text) {
            if (text == null || text.isEmpty())
                return;

            DeluxeAuctions.getInstance().dataHandler.debug("UPDATE Stats: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
            UUID uuid = UUID.fromString(text);
            Bukkit.getScheduler().runTaskLater(DeluxeAuctions.getInstance(), () -> {
                DeluxeAuctions.getInstance().databaseManager.loadStat(uuid);
            }, 1);
        }
    },

    RELOAD {
        @Override
        public void getMessage(String text) {
            DeluxeAuctions.getInstance().dataHandler.debug("RELOAD &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
            Bukkit.getScheduler().runTaskLaterAsynchronously(DeluxeAuctions.getInstance(), () -> {
                DeluxeAuctions.getInstance().reload();
            }, 1);
        }
    };

    public abstract void getMessage(String text);
}
