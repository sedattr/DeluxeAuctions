package me.sedattr.deluxeauctions.economy;

import me.sedattr.deluxeauctions.others.MaterialHelper;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemEconomy implements EconomyManager {
    private final Material material;

    public ItemEconomy(String material) {
        this.material = MaterialHelper.getMaterial(material);
    }

    public boolean isVanillaItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return true;

            return !meta.hasDisplayName() && !meta.hasLore() && !meta.hasEnchants();
        }

        return true;
    }

    @Override
    public boolean addBalance(OfflinePlayer player, Double count) {
        if (!player.isOnline())
            return false;

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null)
            return false;

        Inventory inventory = onlinePlayer.getInventory();
        int remaining = count.intValue();
        int availableSpace = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item == null)
                availableSpace += material.getMaxStackSize();
            else if (item.getType().equals(material)) {
                if (!isVanillaItem(item))
                    continue;

                availableSpace += item.getMaxStackSize() - item.getAmount();
            }
        }

        if (availableSpace < remaining)
            return false;

        int toAdd = remaining;

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType().equals(material)) {
                if (!isVanillaItem(item))
                    continue;

                int maxStackSize = item.getMaxStackSize();
                int spaceLeft = maxStackSize - item.getAmount();

                if (spaceLeft > 0) {
                    int addHere = Math.min(spaceLeft, toAdd);
                    item.setAmount(item.getAmount() + addHere);
                    toAdd -= addHere;

                    if (toAdd == 0)
                        return true;
                }
            }
        }

        while (toAdd > 0) {
            int addHere = Math.min(toAdd, material.getMaxStackSize());
            ItemStack newItem = new ItemStack(material, addHere);

            if (inventory.addItem(newItem).isEmpty())
                toAdd -= addHere;
            else
                return false;
        }

        return true;
    }

    @Override
    public boolean removeBalance(OfflinePlayer player, Double count) {
        if (!player.isOnline())
            return false;

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null)
            return false;

        int balance = (int) getBalance(player);
        int amountToRemove = count.intValue();

        if (balance < amountToRemove)
            return false;

        for (ItemStack item : onlinePlayer.getInventory().getContents()) {
            if (item != null && item.getType().equals(material)) {
                if (!isVanillaItem(item))
                    continue;

                int stackAmount = item.getAmount();

                if (stackAmount > amountToRemove) {
                    item.setAmount(stackAmount - amountToRemove);
                    return true;
                } else {
                    onlinePlayer.getInventory().removeItem(item);
                    amountToRemove -= stackAmount;
                }

                if (amountToRemove <= 0)
                    return true;
            }
        }

        return false;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!player.isOnline())
            return 0;

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null)
            return 0;

        int total = 0;

        for (ItemStack item : onlinePlayer.getInventory().getContents()) {
            if (item != null && item.getType().equals(this.material)) {
                if (!isVanillaItem(item))
                    continue;

                total += item.getAmount();
            }
        }

        return total;
    }
}