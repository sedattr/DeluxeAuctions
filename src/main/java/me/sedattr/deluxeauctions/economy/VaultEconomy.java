package me.sedattr.deluxeauctions.economy;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

public class VaultEconomy implements EconomyManager {
    public boolean addBalance(OfflinePlayer player, Double count) {
        EconomyResponse r = DeluxeAuctions.getInstance().economy.depositPlayer(player, count);
        return r.transactionSuccess();
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        EconomyResponse r = DeluxeAuctions.getInstance().economy.withdrawPlayer(player, count);
        return r.transactionSuccess();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return DeluxeAuctions.getInstance().economy.getBalance(player);
    }
}
