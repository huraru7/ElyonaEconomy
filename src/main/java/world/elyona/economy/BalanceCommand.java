package world.elyona.economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final EconomyCache cache;
    private final EconomyConfig config;

    public BalanceCommand(EconomyCache cache, EconomyConfig config) {
        this.cache = cache;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        long balance = cache.getBalance(player.getUniqueId());
        player.sendMessage(config.getCurrencySymbol() + " " + balance + " " + config.getCurrencyName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of(); // 引数を取らないコマンドのため常に空
    }
}
