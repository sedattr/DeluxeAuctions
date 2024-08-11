package me.sedattr.auctionsapi.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemPreviewEvent extends Event implements Cancellable {
    @Getter private final Player player;
    @Getter private final ItemStack item;
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();

    public ItemPreviewEvent(Player player, ItemStack item) {
        this.player = player;
        this.item = item;
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
