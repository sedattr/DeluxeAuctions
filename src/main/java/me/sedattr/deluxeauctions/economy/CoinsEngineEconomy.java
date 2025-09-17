package me.sedattr.deluxeauctions.economy;

import org.bukkit.OfflinePlayer;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

public class CoinsEngineEconomy implements EconomyManager {
    Currency currency;

    public CoinsEngineEconomy(String name) {
        this.currency = CoinsEngineAPI.getCurrency(name);
    }

    @Override
    public boolean addBalance(OfflinePlayer player, Double count) {
        return CoinsEngineAPI.addBalance(player.getUniqueId(), this.currency, count);
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        return CoinsEngineAPI.removeBalance(player.getUniqueId(), this.currency, count);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return CoinsEngineAPI.getBalance(player.getUniqueId(), this.currency);
    }
}
