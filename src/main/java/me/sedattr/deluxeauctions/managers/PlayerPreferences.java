package me.sedattr.deluxeauctions.managers;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.Getter;
import lombok.Setter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.api.events.AuctionCollectEvent;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.cache.CategoryCache;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
public class PlayerPreferences {
    private final UUID player;

    // for auctions menu
    private AuctionType auctionType = DeluxeAuctions.getInstance().auctionType;
    private SortType sortType = DeluxeAuctions.getInstance().sortType;
    private String search = "";
    private Category category = CategoryCache.getCategories().get(DeluxeAuctions.getInstance().category);
    private int page = 1;
    private int categoryPage = 1;

    // for create menu
    private AuctionType createType = DeluxeAuctions.getInstance().createType;
    private double createPrice = DeluxeAuctions.getInstance().createPrice;
    private long createTime = DeluxeAuctions.getInstance().createTime;

    public PlayerPreferences(UUID player) {
        this.player = player;
    }

    public void updateCreate(ItemStack item) {
        PlayerCache.setItem(this.player, item);

        this.createPrice = DeluxeAuctions.getInstance().createPrice;
        this.createTime = DeluxeAuctions.getInstance().createTime;

        DeluxeAuctions.getInstance().databaseManager.saveItem(this.player, item);
    }

    public void collectAuctions(Player player) {
        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return;
        }

        AtomicDouble money = new AtomicDouble();
        AtomicInteger item = new AtomicInteger();

        List<Auction> playerAuctions = AuctionCache.getOwnedAuctions(this.player);
        List<Auction> auctions = new ArrayList<>(playerAuctions);
        for (Auction auction : auctions) {
            if (auction == null)
                continue;

            String result = auction.sellerCollect(player);
            if (result.isEmpty())
                return;

            PlayerBid playerBid = auction.getAuctionBids().getHighestBid();
            if (playerBid == null)
                item.getAndIncrement();
            else
                money.updateAndGet(v -> v + playerBid.getBidPrice());
        }

        DeluxeAuctions.getInstance().dataHandler.writeToLog("[SELLER COLLECTED ALL AUCTIONS] " + player.getName() + " (" + player.getUniqueId() + ") collected " + money + " COINS and " + item + " ITEMS from auction!");
        if (money.get() > 0.0)
            Utils.sendMessage(player, "seller_collected_moneys", new PlaceholderUtil()
                    .addPlaceholder("%total_money_amount%", DeluxeAuctions.getInstance().numberFormat.format(money.get())));

        if (item.get() > 0)
            Utils.sendMessage(player, "seller_collected_items", new PlaceholderUtil()
                    .addPlaceholder("%total_item_amount%", String.valueOf(item.get())));
    }

    public void collectBids(Player player) {
        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return;
        }

        AtomicDouble money = new AtomicDouble();
        AtomicInteger item = new AtomicInteger();

        List<Auction> playerAuctions = AuctionCache.getBidAuctions(this.player);
        List<Auction> auctions = new ArrayList<>(playerAuctions);
        for (Auction auction : auctions) {
            if (auction == null)
                continue;

            String result = auction.buyerCollect(player);
            if (result.isEmpty())
                return;

            PlayerBid playerBid = auction.getAuctionBids().getPlayerBid(player.getUniqueId());
            if (auction.getAuctionBids().getHighestBid() == playerBid)
                item.getAndIncrement();
            else
                money.updateAndGet(v -> v + playerBid.getBidPrice());
        }

        DeluxeAuctions.getInstance().dataHandler.writeToLog("[BUYER COLLECTED ALL BIDS] " + player.getName() + " (" + player.getUniqueId() + ") collected " + money + " COINS and " + item + " ITEMS from auction!");
        if (money.get() > 0.0)
            Utils.sendMessage(player, "buyer_collected_moneys", new PlaceholderUtil()
                    .addPlaceholder("%total_money_amount%", DeluxeAuctions.getInstance().numberFormat.format(money.get())));

        if (item.get() > 0)
            Utils.sendMessage(player, "buyer_collected_items", new PlaceholderUtil()
                    .addPlaceholder("%total_item_amount%", String.valueOf(item.get())));
    }
}
