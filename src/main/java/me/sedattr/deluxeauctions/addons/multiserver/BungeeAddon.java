package me.sedattr.deluxeauctions.addons.multiserver;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class BungeeAddon implements MultiServerManager, PluginMessageListener {
    String channel = "my:deluxeauctions";

    public BungeeAddon() {
        DeluxeAuctions.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(DeluxeAuctions.getInstance(), this.channel);
        DeluxeAuctions.getInstance().getServer().getMessenger().registerIncomingPluginChannel(DeluxeAuctions.getInstance(), this.channel, this);
    }

    public void publish(String text) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(text);

        DeluxeAuctions.getInstance().dataHandler.debug("SENT Bungee Message: &f" + text + " &8(%level_color%Multi Server&8)", Logger.LogLevel.INFO);
        Iterables.getFirst(Bukkit.getOnlinePlayers(), null).sendPluginMessage(DeluxeAuctions.getInstance(), this.channel, out.toByteArray());
    }

    public boolean checkAuction(String uuid) {
        return true;
    }

    @Override
    public void removeAuction(String uuid) {

    }

    public void deleteAuction(String uuid) {
        publish("DELETE_AUCTION:" + uuid);
    }

    public void updateAuction(String uuid) {
        publish("UPDATE_AUCTION:" + uuid);
    }

    public void updateStat(String uuid) {
        publish("UPDATE_STAT:" + uuid);
    }

    public void reload() {
        publish("RELOAD");
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes) {
        if (!channel.equals(this.channel))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String text = in.readUTF();
        handleMessage(text);
    }
}
