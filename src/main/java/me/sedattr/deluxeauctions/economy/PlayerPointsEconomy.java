package me.sedattr.deluxeauctions.economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PlayerPointsEconomy implements EconomyManager {
    public final PlayerPointsAPI api;

    public PlayerPointsEconomy() {
        this.api = PlayerPoints.getInstance().getAPI();
    }

    @Override
    public boolean addBalance(OfflinePlayer player, Double count) {
        return this.api.give(player.getUniqueId(), (int) Math.round(count));
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        return this.api.take(player.getUniqueId(), (int) Math.round(count));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return this.api.look(player.getUniqueId());
    }
}