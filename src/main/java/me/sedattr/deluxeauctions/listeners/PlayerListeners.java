package me.sedattr.deluxeauctions.listeners;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryVariables;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListeners implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        DeluxeAuctions.getInstance().databaseManager.loadStat(player.getUniqueId());
        DeluxeAuctions.getInstance().databaseManager.loadItem(player.getUniqueId());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        PlayerCache.removeItem(player.getUniqueId());
        PlayerCache.removePreferences(player.getUniqueId());
        PlayerCache.removeStats(player.getUniqueId());
        InventoryVariables.removeCooldown(player);
        InventoryVariables.removePlayerInventory(player);
    }
}
