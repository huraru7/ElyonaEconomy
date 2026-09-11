package world.elyona.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyConfig {

    private final JavaPlugin plugin;

    private String currencyName;
    private String currencySymbol;
    private long initialGrant;
    private double transactionTax;

    public EconomyConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        currencyName = cfg.getString("economy.currency_name", "Cred");
        currencySymbol = cfg.getString("economy.currency_symbol", "Cr");
        initialGrant = cfg.getLong("economy.initial_grant", 500L);
        transactionTax = cfg.getDouble("economy.transaction_tax", 0.05);
    }

    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public long getInitialGrant() { return initialGrant; }
    public double getTransactionTax() { return transactionTax; }
}
