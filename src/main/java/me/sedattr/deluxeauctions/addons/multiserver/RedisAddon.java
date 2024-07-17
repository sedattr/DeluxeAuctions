package me.sedattr.deluxeauctions.addons.multiserver;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Logger;
import me.sedattr.deluxeauctionsredis.RedisManager;
import me.sedattr.deluxeauctionsredis.RedisPlugin;

public class RedisAddon implements MultiServerManager {
    private final RedisManager redisManager;

    public RedisAddon() {
        this.redisManager = RedisPlugin.getInstance().getRedisManager();
        this.redisManager.connect();
    }

    public void publish(String text) {
        DeluxeAuctions.getInstance().dataHandler.debug("SENT Redis Message: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);

        this.redisManager.connect();
        this.redisManager.publish(text);
    }

    public void removeAuction(String uuid) {
        this.redisManager.removeAuction(uuid);
    }

    public boolean checkAuction(String uuid) {
        return this.redisManager.isUpdating(uuid);
    }

    public void deleteAuction(String uuid) {
        publish("AUCTION_DELETE:" + uuid);
    }

    public void updateAuction(String uuid) {
        publish("AUCTION_UPDATE:" + uuid);
    }

    public void updateStat(String uuid) {
        publish("STATS_UPDATE:" + uuid);
    }

    public void reload() {
        publish("RELOAD");
    }
}