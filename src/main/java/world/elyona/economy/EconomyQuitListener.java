package world.elyona.economy;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EconomyQuitListener implements Listener {

    private final EconomyCache cache;

    public EconomyQuitListener(EconomyCache cache) {
        this.cache = cache;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cache.flush(event.getPlayer().getUniqueId());
    }
}
