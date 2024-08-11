package me.sedattr.auctionsapi.cache;

import com.google.common.collect.Maps;
import lombok.Getter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.*;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionCache {
    @Getter private static final HashMap<UUID, Auction> auctions = Maps.newHashMap();
    private static final Set<UUID> updatedAuctions = new HashSet<>();

    public static boolean isAuctionUpdating(UUID auctionUUID) {
        return updatedAuctions.contains(auctionUUID);
    }

    public static void addUpdatingAuction(UUID auctionUUID) {
        updatedAuctions.add(auctionUUID);
    }

    public static void removeUpdatingAuction(UUID auctionUUID) {
        updatedAuctions.remove(auctionUUID);
    }

    public static Auction getAuction(UUID uuid) {
        return auctions.get(uuid);
    }

    public static void addAuction(Auction auction) {
        if (auction == null)
            return;

        auctions.put(auction.getAuctionUUID(), auction);
    }

    public static void removeAuction(UUID uuid) {
        if (!auctions.containsKey(uuid))
            return;

        auctions.remove(uuid);
    }

    public static List<Auction> getOwnedAuctions(UUID uuid) {
        if (auctions.isEmpty())
            return Collections.emptyList();

        return auctions.values().stream().filter(auction -> !auction.isSellerClaimed() && auction.getAuctionOwner().equals(uuid)).toList();
    }

    public static List<Auction> getBidAuctions(UUID uuid) {
        if (auctions.isEmpty())
            return Collections.emptyList();

        return auctions.values().stream().filter(auction -> {
            PlayerBid playerBid = auction.getAuctionBids().getPlayerBid(uuid);
            if (playerBid == null)
                return false;

            return !playerBid.isCollected();
        }).toList();
    }

    public static List<Auction> getFilteredAuctions(AuctionType type, Category category, String search) {
        if (auctions.isEmpty())
            return Collections.emptyList();

        ConcurrentHashMap.KeySetView<Auction, Boolean> result = ConcurrentHashMap.newKeySet();


        auctions.values().parallelStream().forEach(auction -> {
            if (type != AuctionType.ALL && auction.getAuctionType() != type)
                return;
            if (auction.isEnded())
                return;
            ItemStack itemStack = auction.getAuctionItem();
            if (itemStack == null)
                return;
            if (category != null && !category.isGlobal())
                if (!auction.getAuctionCategory().equals(category.getName()))
                    return;

            if (search != null && !search.isEmpty()) {
                String lowerCaseSearch = search.toLowerCase();

                ItemMeta meta = itemStack.getItemMeta();
                if (meta instanceof EnchantmentStorageMeta) {
                    if (EnchantCache.isEnchantmentAdded(((EnchantmentStorageMeta) meta).getStoredEnchants(), lowerCaseSearch)) {
                        result.add(auction);
                        return;
                }
                } else {
                    if (EnchantCache.isEnchantmentAdded(itemStack.getEnchantments(), lowerCaseSearch)) {
                        result.add(auction);
                        return;
                    }
                }

                if (meta != null) {
                    if (meta.getDisplayName() != null) {
                        if (Utils.strip(meta.getDisplayName().toLowerCase()).contains(lowerCaseSearch)) {
                            result.add(auction);
                            return;
                        }
                    }

                    if (DeluxeAuctions.getInstance().version >= 21) {
                        String itemName = meta.getItemName();
                        if (itemName != null && !itemName.isEmpty()) {
                            if (Utils.strip(itemName.toLowerCase()).contains(lowerCaseSearch)) {
                                result.add(auction);
                                return;
                            }
                        }
                    }
                }

                if (itemStack.getType().name().replace("_", " ").toLowerCase().contains(lowerCaseSearch))
                    result.add(auction);

                return;
            } else if (category != null && category.isGlobal()) {
                result.add(auction);
                return;
            }

            result.add(auction);
        });

        return new ArrayList<>(result);
    }

    public static List<Auction> getOnGoingAuctions(List<Auction> filteredAuctions, SortType sort, int page, int slot) {
        if (auctions.isEmpty())
            return Collections.emptyList();

        int lower = (page - 1) * slot;
        if (filteredAuctions.isEmpty() || lower >= filteredAuctions.size())
            return Collections.emptyList();

        switch (sort) {
            case LOWEST_PRICE -> filteredAuctions.sort(Comparator.comparing(Auction::getAuctionPrice));
            case HIGHEST_PRICE -> {
                filteredAuctions.sort(Comparator.comparing(Auction::getAuctionPrice));
                Collections.reverse(filteredAuctions);
            }
            case ALPHABETICAL -> filteredAuctions.sort((a, b) -> Utils.strip(Utils.getDisplayName(a.getAuctionItem())).compareToIgnoreCase(Utils.strip(Utils.getDisplayName(b.getAuctionItem()))));
            case MOST_BIDS -> {
                filteredAuctions.sort(Comparator.comparingInt(a -> a.getAuctionBids().getPlayerBids().size()));
                Collections.reverse(filteredAuctions);
            }
            case ENDING_SOON -> filteredAuctions.sort(Comparator.comparing(Auction::getAuctionEndTime));
            case RANDOM -> Collections.shuffle(filteredAuctions);
        }

        int upper = Math.min(page * slot, filteredAuctions.size());
        List<Auction> newAuctions = new ArrayList<>(upper - lower);
        for (int i = lower; i < upper; i++) {
            newAuctions.add(filteredAuctions.get(i));
        }

        return newAuctions;
    }
}
