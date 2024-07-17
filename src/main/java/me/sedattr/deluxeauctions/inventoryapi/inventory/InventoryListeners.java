package me.sedattr.deluxeauctions.inventoryapi.inventory;

import me.sedattr.deluxeauctions.inventoryapi.item.ClickInterface;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.time.ZonedDateTime;

public class InventoryListeners implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        HInventory gui = InventoryAPI.getInventory(player);
        if (gui == null)
            return;
        event.setCancelled(true);

        Inventory inventory = event.getClickedInventory();
        if (inventory == null || event.getSlot() < 0)
            return;

        if (!inventory.equals(gui.getInventory()))
            return;

        long cooldown = InventoryVariables.getCooldown(player);
        if (cooldown > 0) {
            long time = ZonedDateTime.now().toInstant().toEpochMilli() - cooldown;
            if (time < 400) {
                Utils.sendMessage(player, "click_cooldown");
                return;
            }
        }

        ClickableItem clickableItem = gui.getItem(event.getSlot());
        if (clickableItem == null)
            return;

        ClickInterface click = clickableItem.getClick();
        if (click == null)
            return;

        click.click(event);
        InventoryVariables.addCooldown(player, ZonedDateTime.now().toInstant().toEpochMilli());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        HInventory gui = InventoryAPI.getInventory(player);
        if (gui == null)
            return;

        if (gui.isCloseable()) {
            InventoryVariables.removePlayerInventory(player);
            return;
        }
        if (InventoryAPI.getInstance() == null)
            return;

        Bukkit.getScheduler().runTaskLater(InventoryAPI.getInstance(), () -> gui.open(player), 1L);
    }
}