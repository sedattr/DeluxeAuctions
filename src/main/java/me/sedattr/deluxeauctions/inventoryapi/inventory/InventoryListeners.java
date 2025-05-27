package me.sedattr.deluxeauctions.inventoryapi.inventory;

import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.auctionsapi.events.ItemPreviewEvent;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickInterface;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.menus.CreateMenu;
import me.sedattr.deluxeauctions.others.TaskUtils;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;

public class InventoryListeners implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof HInventory))
            return;
        event.setCancelled(true);

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null)
            return;

        int slot = event.getSlot();
        if (slot < 0)
            return;

        HInventory gui = InventoryAPI.getInventory(player);
        if (gui == null)
            return;

        long cooldown = InventoryVariables.getCooldown(player);
        if (cooldown > 0) {
            long time = ZonedDateTime.now().toInstant().toEpochMilli() - cooldown;
            if (time < 400) {
                Utils.sendMessage(player, "click_cooldown");
                return;
            }
        }

        if (!clickedInventory.equals(gui.getInventory())) {
            String type = gui.getId();
            if (type == null || type.isEmpty())
                return;

            if (!type.equals("main") && !type.equals("create"))
                return;

            ItemStack item = clickedInventory.getItem(slot);
            if (item == null || item.getType().equals(Material.AIR))
                return;

            String sellable = AuctionHook.isSellable(player, item);
            if (!sellable.isEmpty()) {
                Utils.sendMessage(player, sellable);
                return;
            }

            ItemPreviewEvent previewEvent = new ItemPreviewEvent(player, item);
            Bukkit.getPluginManager().callEvent(previewEvent);
            if (previewEvent.isCancelled())
                return;

            PlayerPreferences preferences = PlayerCache.getPreferences(player.getUniqueId());
            boolean status = preferences.updateCreateItem(player, slot, true);
            if (!status)
                return;

            Utils.playSound(player, "inventory_item_click");
            new CreateMenu(player).open("main");

            InventoryVariables.addCooldown(player, ZonedDateTime.now().toInstant().toEpochMilli());
            return;
        }

        ClickableItem clickableItem = gui.getItem(slot);
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

        TaskUtils.runLater(() -> gui.open(player), 1L);
    }
}