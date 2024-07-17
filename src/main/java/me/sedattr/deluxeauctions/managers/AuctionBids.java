package me.sedattr.deluxeauctions.managers;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class AuctionBids {
    private List<PlayerBid> playerBids = new ArrayList<>();
    private PlayerBid highestBid;

    public boolean isAllCollected() {
        List<PlayerBid> bids = getHighestPlayerBids();
        for (PlayerBid bid : bids) {
            if (!bid.isCollected())
                return false;
        }

        return true;
    }

    public void addPlayerBids(List<PlayerBid> bids) {
        PlayerBid highest = null;
        for (PlayerBid bid : bids) {
            if (highest == null || bid.getBidPrice() > highest.getBidPrice())
                highest = bid;
        }

        this.highestBid = highest;
        this.playerBids = bids;
    }

    public void addPlayerBid(PlayerBid playerBid) {
        this.playerBids.add(playerBid);

        if (this.highestBid == null || playerBid.getBidPrice() > this.highestBid.getBidPrice())
            this.highestBid = playerBid;
    }

    public List<PlayerBid> getHighestPlayerBids() {
        if (this.playerBids.isEmpty())
            return playerBids;

        HashMap<PlayerBid, UUID> bids = Maps.newHashMap();
        this.playerBids.sort(Comparator.comparing(PlayerBid::getBidPrice).reversed());
        for (PlayerBid playerBid : this.playerBids) {
            if (bids.containsValue(playerBid.getBidOwner()))
                continue;

            bids.put(playerBid, playerBid.getBidOwner());
        }

        return bids.keySet().stream().toList();
    }

    public PlayerBid getPlayerBid(UUID uuid) {
        if (this.playerBids.isEmpty())
            return null;

        this.playerBids.sort(Comparator.comparing(PlayerBid::getBidPrice).reversed());
        for (PlayerBid playerBid : this.playerBids) {
            UUID bidOwner = playerBid.getBidOwner();
            if (bidOwner.equals(uuid))
                return playerBid;
        }

        return null;
    }
}