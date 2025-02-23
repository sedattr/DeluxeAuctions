package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

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

    private static void invokeFoliaMethod(String methodName, Plugin plugin, Consumer<Object> task, long delayTicks) {
        try {
            Method getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object scheduler = getGlobalRegionScheduler.invoke(null);
            Method method = scheduler.getClass().getMethod(methodName, Plugin.class, Consumer.class, long.class);
            method.invoke(scheduler, plugin, task, delayTicks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia) {
            invokeFoliaMethod("execute", DeluxeAuctions.getInstance(), t -> runnable.run(), 0L);
        } else {
            Bukkit.getScheduler().runTask(DeluxeAuctions.getInstance(), runnable);
        }
    }

    public static void runAsync(Runnable runnable) {
        if (isFolia) {
            invokeFoliaMethod("execute", DeluxeAuctions.getInstance(), t -> runnable.run(), 0L);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(DeluxeAuctions.getInstance(), runnable);
        }
    }

    public static void runLater(Runnable runnable, long delayTicks) {
        if (isFolia) {
            invokeFoliaMethod("runDelayed", DeluxeAuctions.getInstance(), t -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(DeluxeAuctions.getInstance(), runnable, delayTicks);
        }
    }

    public static void runLaterAsync(Runnable runnable, long delayTicks) {
        if (isFolia) {
            invokeFoliaMethod("runDelayed", DeluxeAuctions.getInstance(), t -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(DeluxeAuctions.getInstance(), runnable, delayTicks);
        }
    }

    public static void runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            invokeFoliaMethod("runAtFixedRate", DeluxeAuctions.getInstance(), t -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), runnable, delayTicks, periodTicks);
        }
    }

    public static void runTimerAsync(Player player, String id, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            try {
                Method getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
                Object scheduler = getGlobalRegionScheduler.invoke(null);
                Method runAtFixedRate = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

                Object[] taskHolder = new Object[1];
                taskHolder[0] = runAtFixedRate.invoke(scheduler, DeluxeAuctions.getInstance(), (Consumer<Object>) task -> {
                    HInventory inventory = InventoryAPI.getInventory(player);
                    if (inventory == null) {
                        cancelTask(taskHolder[0]);
                        return;
                    }

                    String inventoryId = inventory.getId();
                    if (!inventoryId.equalsIgnoreCase(id)) {
                        if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                            runnable.run();
                            return;
                        }

                        cancelTask(taskHolder[0]);
                        return;
                    }

                    runnable.run();
                }, delayTicks, periodTicks);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), () -> {
                HInventory inventory = InventoryAPI.getInventory(player);
                if (inventory == null) {
                    return;
                }

                String inventoryId = inventory.getId();
                if (!inventoryId.equalsIgnoreCase(id)) {
                    if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                        runnable.run();
                        return;
                    }
                    return;
                }
                runnable.run();
            }, delayTicks, periodTicks);
        }
    }

    private static void cancelTask(Object task) {
        try {
            Method cancel = task.getClass().getMethod("cancel");
            cancel.invoke(task);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}