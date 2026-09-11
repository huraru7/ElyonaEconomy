package world.elyona.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.mimic.MimicMessenger;

public class EconomyJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final EconomyCache cache;
    private final EconomyRepository repository;
    private final EconomyConfig config;
    private final MimicMessenger mimic;

    public EconomyJoinListener(JavaPlugin plugin, EconomyCache cache, EconomyRepository repository,
                                EconomyConfig config, MimicMessenger mimic) {
        this.plugin = plugin;
        this.cache = cache;
        this.repository = repository;
        this.config = config;
        this.mimic = mimic;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        repository.ensureAccount(player.getUniqueId())
                .thenCompose(v -> cache.load(player.getUniqueId()))
                .thenCompose(balance -> repository.isInitialGrantDone(player.getUniqueId()))
                .thenAccept(done -> {
                    if (done) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        long grant = config.getInitialGrant();
                        cache.addBalance(player.getUniqueId(), grant);
                        repository.markInitialGrantDone(player.getUniqueId());
                        mimic.sendTo(player, "初回参加ボーナスとして " + grant + " Cr を受け取りました！");
                        plugin.getLogger().info("[初回ボーナス] " + player.getName() + " に " + grant + " Cr 付与");
                    });
                })
                .exceptionally(e -> {
                    plugin.getLogger().warning("経済データ読み込みエラー(" + player.getName() + "): " + e.getMessage());
                    return null;
                });
    }
}
