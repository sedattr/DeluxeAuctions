package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class TaskUtils {
    private static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (final ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .execute(DeluxeAuctions.getInstance(), runnable);
        else
            Bukkit.getScheduler().runTask(DeluxeAuctions.getInstance(), runnable);
    }

    public static void runAsync(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .execute(DeluxeAuctions.getInstance(), runnable);
        else
            Bukkit.getScheduler().runTaskAsynchronously(DeluxeAuctions.getInstance(), runnable);
    }

    public static void runLater(Runnable runnable, long delayTicks) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .runDelayed(DeluxeAuctions.getInstance(), t -> runnable.run(), delayTicks);
       else
            Bukkit.getScheduler().runTaskLater(DeluxeAuctions.getInstance(), runnable, delayTicks);
    }

    public static void runLaterAsync(Runnable runnable, long delayTicks) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .runDelayed(DeluxeAuctions.getInstance(), t -> runnable.run(), delayTicks);
        else
            Bukkit.getScheduler().runTaskLaterAsynchronously(DeluxeAuctions.getInstance(), runnable, delayTicks);
    }

    public static void runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(DeluxeAuctions.getInstance(), (t) -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks);
        else
            Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), runnable, delayTicks < 1 ? 1 : delayTicks, periodTicks);
    }

    public static void runTimerAsync(Player player, String id, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            final io.papermc.paper.threadedregions.scheduler.ScheduledTask[] taskHolder = new io.papermc.paper.threadedregions.scheduler.ScheduledTask[1];

            taskHolder[0] = Bukkit.getGlobalRegionScheduler().runAtFixedRate(DeluxeAuctions.getInstance(), task -> {
                HInventory inventory = InventoryAPI.getInventory(player);
                if (inventory == null) {
                    taskHolder[0].cancel();
                    return;
                }

                String inventoryId = inventory.getId();
                if (!inventoryId.equalsIgnoreCase(id)) {
                    if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                        runnable.run();
                        return;
                    }

                    taskHolder[0].cancel();
                    return;
                }

                runnable.run();
            }, delayTicks, periodTicks);

        }
        else {
            final BukkitTask[] taskHolder = new BukkitTask[1];

            taskHolder[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), () -> {
                HInventory inventory = InventoryAPI.getInventory(player);
                if (inventory == null) {
                    taskHolder[0].cancel();
                    return;
                }

                String inventoryId = inventory.getId();
                if (!inventoryId.equalsIgnoreCase(id)) {
                    if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                        runnable.run();
                        return;
                    }

                    taskHolder[0].cancel();
                    return;
                }

                runnable.run();
            }, delayTicks, periodTicks);
        }
    }
}