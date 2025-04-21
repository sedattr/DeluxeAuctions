package me.sedattr.auctionsapi.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InputOpenEvent extends Event implements Cancellable {
    @Getter private final Player player;
    @Getter private final String menu;
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();

    public InputOpenEvent(Player player, String menu) {
        this.player = player;
        this.menu = menu;
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
