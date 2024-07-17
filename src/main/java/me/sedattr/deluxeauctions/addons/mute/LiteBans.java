package me.sedattr.deluxeauctions.addons.mute;

import litebans.api.Database;
import org.bukkit.entity.Player;
import java.net.InetSocketAddress;

public class LiteBans implements MuteManager {
    public boolean isMuted(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null)
            return false;

        return Database.get().isPlayerMuted(player.getUniqueId(), address.getAddress().getHostAddress());
    }
}