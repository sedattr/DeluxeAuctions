package me.sedattr.deluxeauctions.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy implements EconomyManager {
    private Economy economy;

    public VaultEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return;

        this.economy = rsp.getProvider();
    }

    public boolean addBalance(OfflinePlayer player, Double count) {
        return this.economy.depositPlayer(player, count).transactionSuccess();
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        return this.economy.withdrawPlayer(player, count).transactionSuccess();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return this.economy.getBalance(player);
    }
}