package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TaskUtils {
    public static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (final ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().execute(DeluxeAuctions.getInstance(), runnable);
        } else {
            Bukkit.getScheduler().runTask(DeluxeAuctions.getInstance(), runnable);
        }
    }

    public static void runAsync(Runnable runnable) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().execute(DeluxeAuctions.getInstance(), runnable);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(DeluxeAuctions.getInstance(), runnable);
        }
    }

    public static void runLater(Runnable runnable, long delayTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().runDelayed(DeluxeAuctions.getInstance(), task -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(DeluxeAuctions.getInstance(), runnable, delayTicks);
        }
    }

    public static void runLaterAsync(Runnable runnable, long delayTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().runDelayed(DeluxeAuctions.getInstance(), task -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(DeluxeAuctions.getInstance(), runnable, delayTicks);
        }
    }

    public static void runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().runAtFixedRate(DeluxeAuctions.getInstance(), task -> runnable.run(), delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), runnable, delayTicks, periodTicks);
        }
    }

    public static void runTimerAsync(Player player, String id, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().runAtFixedRate(DeluxeAuctions.getInstance(), task -> {
                HInventory inventory = InventoryAPI.getInventory(player);
                if (inventory == null) {
                    cancelTask(task);
                    return;
                }

                String inventoryId = inventory.getId();
                if (!inventoryId.equalsIgnoreCase(id)) {
                    if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                        runnable.run();
                        return;
                    }

                    cancelTask(task);
                    return;
                }

                runnable.run();
            }, delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), (task) -> {
                HInventory inventory = InventoryAPI.getInventory(player);
                if (inventory == null) {
                    task.cancel();
                    return;
                }

                String inventoryId = inventory.getId();
                if (!inventoryId.equalsIgnoreCase(id)) {
                    if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                        runnable.run();
                        return;
                    }

                    task.cancel();
                    return;
                }
                runnable.run();
            }, delayTicks, periodTicks);
        }
    }

    private static void cancelTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        if (!isFolia)
            return;

        task.cancel();
    }
}