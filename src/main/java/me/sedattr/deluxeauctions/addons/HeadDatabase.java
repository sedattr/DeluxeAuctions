package me.sedattr.deluxeauctions.addons;

import lombok.Getter;
import lombok.Setter;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Setter
@Getter
public class HeadDatabase implements Listener {
    private HeadDatabaseAPI hdbAPI = new HeadDatabaseAPI();

    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent e) {
        this.hdbAPI = new HeadDatabaseAPI();
    }
}
