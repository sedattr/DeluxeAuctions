package me.sedattr.deluxeauctions.addons.mute;

import me.leoko.advancedban.Universal;
import org.bukkit.entity.Player;

public class AdvancedBan implements MuteManager {
    public boolean isMuted(Player player) {
        return Universal.get().getMethods().callChat(player);
    }
}