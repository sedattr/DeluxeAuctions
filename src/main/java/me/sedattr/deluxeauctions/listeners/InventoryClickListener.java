package me.sedattr.deluxeauctions.listeners;

import me.sedattr.deluxeauctions.api.AuctionHook;
import me.sedattr.deluxeauctions.api.events.ItemPreviewEvent;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryVariables;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.menus.CreateMenu;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;

public class InventoryClickListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;

        HInventory gui = InventoryAPI.getInventory(player);
        if (gui == null)
            return;
        e.setCancelled(true);

        if (e.getAction() == InventoryAction.DROP_ALL_SLOT || e.getAction() == InventoryAction.DROP_ONE_SLOT)
            return;

        Inventory inventory = e.getClickedInventory();
        if (inventory == null)
            return;
        if (gui.getInventory().equals(inventory))
            return;

        String type = gui.getId();
        if (type == null || type.isEmpty())
            return;

        if (!type.equals("main") && !type.equals("create"))
            return;

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().equals(Material.AIR))
            return;

        ItemStack createItem = PlayerCache.getItem(player.getUniqueId());
        if (type.equals("main") && createItem != null)
            return;

        long cooldown = InventoryVariables.getCooldown(player);
        if (cooldown > 0) {
            long time = ZonedDateTime.now().toInstant().toEpochMilli() - cooldown;
            if (time < 500) {
                Utils.sendMessage(player, "click_cooldown");
                return;
            }
        }

        String sellable = AuctionHook.isSellable(player, item);
        if (!sellable.isEmpty()) {
            Utils.sendMessage(player, sellable);
            return;
        }

        ItemPreviewEvent event = new ItemPreviewEvent(player, item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;


        ItemStack clone = item.clone();
        player.getInventory().removeItem(item);

        if (type.equals("create") && createItem != null)
            player.getInventory().addItem(createItem);

        Utils.playSound(player, "inventory_item_click");

        PlayerPreferences preferences = PlayerCache.getPreferences(player.getUniqueId());
        preferences.updateCreate(clone);

        new CreateMenu(player).open("main");
        InventoryVariables.addCooldown(player, ZonedDateTime.now().toInstant().toEpochMilli());
    }
}