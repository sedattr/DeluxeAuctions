package me.sedattr.deluxeauctions.addons.multiserver;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Logger;

import java.util.UUID;

public interface MultiServerManager {
    void publish(String text);
    void reload();
    void updateStat(String uuid);
    void updateAuction(String uuid);
    void deleteAuction(String uuid);
    boolean checkAuction(String uuid);
    void removeAuction(String uuid);

    default void handleMessage(String text) {
        if (text == null || text.isEmpty())
            return;

        String[] args = text.split(":", 2);
        if (args.length < 1)
            return;

        String type = args[0];
        if (type.equals("CHECK_AUCTION"))
            return;

        MessageType messageType = MessageType.valueOf(type);

        DeluxeAuctions.getInstance().dataHandler.debug("Handling Message: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.WARN);
        messageType.getMessage(args.length > 1 ? args[1] : "");
    }
}