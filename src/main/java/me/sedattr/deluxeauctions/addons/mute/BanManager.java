package me.sedattr.deluxeauctions.addons.mute;

import me.confuser.banmanager.common.api.BmAPI;
import org.bukkit.entity.Player;

public class BanManager implements MuteManager {
    public boolean isMuted(Player player) {
        return BmAPI.isMuted(player.getUniqueId());
    }
}