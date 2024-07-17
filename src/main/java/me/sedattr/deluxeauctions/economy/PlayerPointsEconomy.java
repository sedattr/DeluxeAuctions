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
        CompletableFuture<Boolean> status = this.api.giveAsync(player.getUniqueId(), Integer.parseInt(String.valueOf(count)));

        try {
            return status.get();
        } catch (InterruptedException | ExecutionException ignored) {
            return false;
        }
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        CompletableFuture<Boolean> status = this.api.takeAsync(player.getUniqueId(), Integer.parseInt(String.valueOf(count)));

        try {
            return status.get();
        } catch (InterruptedException | ExecutionException ignored) {
            return false;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        CompletableFuture<Integer> status = this.api.lookAsync(player.getUniqueId());

        try {
            return Double.valueOf(status.get());
        } catch (InterruptedException | ExecutionException ignored) {
            return 0.0;
        }
    }
}
