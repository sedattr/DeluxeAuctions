package me.sedattr.deluxeauctions.database;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.api.events.AuctionCreateEvent;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.managers.PlayerStats;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;

public interface DatabaseManager {
    void deleteAuction(String uuid);

    void loadAuction(UUID uuid);
    boolean loadAuctions();

    void loadStat(UUID uuid);
    void loadItem(UUID uuid);

    void saveAuction(Auction auction);
    void saveAuctions();

    void saveItem(UUID uuid, ItemStack item);
    void saveStats(PlayerStats stats);

    void shutdown();
}