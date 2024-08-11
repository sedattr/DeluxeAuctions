package me.sedattr.auctionsapi.events;

import lombok.Getter;
import me.sedattr.deluxeauctions.managers.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AuctionPreCollectAllEvent extends Event implements Cancellable {
    @Getter private final Player player;
    @Getter private final List<Auction> auctions;
    @Getter private final boolean isSeller;
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();

    public AuctionPreCollectAllEvent(Player player, List<Auction> auctions, boolean isSeller) {
        this.player = player;
        this.auctions = auctions;
        this.isSeller = isSeller;
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
