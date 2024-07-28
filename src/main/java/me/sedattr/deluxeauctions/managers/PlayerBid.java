package me.sedattr.deluxeauctions.managers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
public class PlayerBid {
    private final UUID uuid;
    private final long bidTime;
    private final double bidPrice;
    private final UUID bidOwner;
    private final String bidOwnerDisplayName;
    @Setter private boolean collected = false;

    public PlayerBid(Player player, double price) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player.getUniqueId();
        this.bidOwnerDisplayName = !player.getDisplayName().equalsIgnoreCase("") ? player.getDisplayName() : player.getName();
        this.bidPrice = price;
        this.bidTime = ZonedDateTime.now().toInstant().getEpochSecond();
    }

    public PlayerBid(Player player, double price, boolean collected) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player.getUniqueId();
        this.bidOwnerDisplayName = !player.getDisplayName().equalsIgnoreCase("") ? player.getDisplayName() : player.getName();
        this.bidPrice = price;
        this.bidTime = ZonedDateTime.now().toInstant().getEpochSecond();
        this.collected = collected;
    }

    public PlayerBid(UUID player, double price, boolean collected) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player;
        this.bidPrice = price;
        this.bidTime = ZonedDateTime.now().toInstant().getEpochSecond();
        this.collected = collected;

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        this.bidOwnerDisplayName = offlinePlayer.getName();
    }

    public PlayerBid(UUID player, String displayName, double price, long time) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player;
        this.bidOwnerDisplayName = displayName;
        this.bidPrice = price;
        this.bidTime = time;
    }

    public PlayerBid(UUID uuid, UUID player, String displayName, double price, long time, boolean collected) {
        this.uuid = uuid;
        this.bidOwner = player;
        this.bidOwnerDisplayName = displayName;
        this.bidPrice = price;
        this.bidTime = time;
        this.collected = collected;
    }

    @Override
    public String toString() {
        return uuid + "," + this.bidOwner + "," + this.bidOwnerDisplayName + "," + this.bidPrice + "," + this.bidTime + "," + this.collected;
    }
}
