package me.sedattr.deluxeauctions.economy;

import com.edwardbelt.edprison.EdPrison;
import com.edwardbelt.edprison.api.models.EconomyModel;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import org.bukkit.OfflinePlayer;

public class EdPrisonEconomy implements EconomyManager {
    private final String economy;
    private final EconomyModel economyAPI;

    public EdPrisonEconomy() {
        this.economyAPI = EdPrison.getInstance().getApi().getEconomyApi();
        this.economy = DeluxeAuctions.getInstance().configFile.getString("economy.edprison_settings.currency_name", "tokens");
    }

    @Override
    public boolean addBalance(OfflinePlayer player, Double count) {
        this.economyAPI.addEco(player.getUniqueId(), this.economy, count);
        return true;
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        this.economyAPI.removeEco(player.getUniqueId(), this.economy, count);
        return true;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return this.economyAPI.getEco(player.getUniqueId(), this.economy);
    }
}
