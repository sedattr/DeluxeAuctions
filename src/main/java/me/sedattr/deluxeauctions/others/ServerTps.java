package me.sedattr.deluxeauctions.others;

public class ServerTps implements Runnable {
    private static int TICK_COUNT = 0;
    private static final long[] TICKS = new long[600];

    public static double getTPS(int ticks) {
        if (TICK_COUNT < ticks) {
            return 20.0D;
        }

        int target = (TICK_COUNT - 1 - ticks) % TICKS.length;
        if (target < 0)
            return 20.0D;

        long elapsed = System.currentTimeMillis() - TICKS[target];

        return ticks / (elapsed / 1000.0D);
    }

    public void run() {
        TICKS[(TICK_COUNT % TICKS.length)] = System.currentTimeMillis();
        TICK_COUNT += 1;
    }
}