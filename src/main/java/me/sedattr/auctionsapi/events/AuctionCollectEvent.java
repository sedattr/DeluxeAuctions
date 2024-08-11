package me.sedattr.auctionsapi.events;

import lombok.Getter;
import me.sedattr.deluxeauctions.managers.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionCollectEvent extends Event implements Cancellable {
    @Getter private final Player player;
    @Getter private final Auction auction;
    @Getter private final boolean isSeller;
    @Getter private final boolean isExpired;
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();

    public AuctionCollectEvent(Player player, Auction auction, boolean expired) {
        this.player = player;
        this.auction = auction;
        this.isSeller = auction.getAuctionOwner().equals(player.getUniqueId());
        this.isExpired = expired;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
