package me.sedattr.deluxeauctions.economy;

import org.bukkit.OfflinePlayer;

public interface EconomyManager {
    boolean addBalance(OfflinePlayer player, Double count);

    boolean removeBalance(OfflinePlayer player, Double count);

    double getBalance(OfflinePlayer player);
}