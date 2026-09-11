package world.elyona.economy;

import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.mimic.MimicMessenger;

public class ElyonaEconomyPlugin extends JavaPlugin {

    private EconomyConfig economyConfig;
    private EconomyRepository economyRepository;
    private EconomyCache economyCache;

    @Override
    public void onEnable() {
        ElyonaCorePlugin core = ElyonaCorePlugin.getInstance();
        MimicMessenger mimic = core.getMimicMessenger();

        economyConfig = new EconomyConfig(this);
        economyRepository = new EconomyRepository(core.getDatabaseManager());
        economyRepository.initialize();
        economyCache = new EconomyCache(core.getDatabaseManager());

        getServer().getPluginManager().registerEvents(
                new EconomyJoinListener(this, economyCache, economyRepository, economyConfig, mimic), this);
        getServer().getPluginManager().registerEvents(
                new EconomyQuitListener(economyCache), this);
        getServer().getPluginManager().registerEvents(
                new RankRewardListener(economyCache, economyConfig, mimic), this);

        getCommand("balance").setExecutor(new BalanceCommand(economyCache, economyConfig));
        getCommand("pay").setExecutor(new PayCommand(this, economyCache, mimic, economyConfig));
        getCommand("eco").setExecutor(new EcoCommand(economyCache, mimic, economyConfig));

        getLogger().info("ElyonaEconomy が有効化されました。");
    }

    @Override
    public void onDisable() {
        if (economyCache != null) {
            economyCache.flushAll();
        }
        getLogger().info("ElyonaEconomy が無効化されました。");
    }

    public EconomyCache getEconomyCache() { return economyCache; }
    public EconomyConfig getEconomyConfig() { return economyConfig; }
}
