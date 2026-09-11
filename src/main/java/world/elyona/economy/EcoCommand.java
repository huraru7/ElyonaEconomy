package world.elyona.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.elyona.core.mimic.MimicMessenger;

import java.util.UUID;

public class EcoCommand implements CommandExecutor {

    private final EconomyCache cache;
    private final MimicMessenger mimic;
    private final EconomyConfig config;

    public EcoCommand(EconomyCache cache, MimicMessenger mimic, EconomyConfig config) {
        this.cache = cache;
        this.mimic = mimic;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("elyona.admin")) {
            if (sender instanceof Player p) mimic.sendTo(p, "権限がありません。");
            else sender.sendMessage("権限がありません。");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("使用方法: /eco <give|take|set> <player> <amount>");
            return true;
        }

        String subCmd = args[0].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid = null;
        String targetName = args[1];

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            // オフラインプレイヤー対応
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            if (offline.hasPlayedBefore()) {
                targetUuid = offline.getUniqueId();
                targetName = offline.getName() != null ? offline.getName() : args[1];
            }
        }

        if (targetUuid == null) {
            sender.sendMessage("プレイヤーが見つかりません: " + args[1]);
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("金額は整数で指定してください。");
            return true;
        }

        String sym = config.getCurrencySymbol();

        switch (subCmd) {
            case "give" -> {
                cache.addBalance(targetUuid, amount);
                sender.sendMessage(targetName + " に " + sym + " " + amount + " を付与しました。残高: " + sym + " " + cache.getBalance(targetUuid));
                if (target != null) mimic.sendTo(target, sym + " " + amount + " が付与されました。残高: " + sym + " " + cache.getBalance(targetUuid));
            }
            case "take" -> {
                long current = cache.getBalance(targetUuid);
                long newBal = Math.max(0, current - amount);
                cache.setBalance(targetUuid, newBal);
                sender.sendMessage(targetName + " から " + sym + " " + (current - newBal) + " を減算しました。残高: " + sym + " " + newBal);
                if (target != null) mimic.sendTo(target, sym + " " + (current - newBal) + " が減算されました。残高: " + sym + " " + newBal);
            }
            case "set" -> {
                if (amount < 0) { sender.sendMessage("金額は0以上を指定してください。"); return true; }
                cache.setBalance(targetUuid, amount);
                sender.sendMessage(targetName + " の残高を " + sym + " " + amount + " に設定しました。");
                if (target != null) mimic.sendTo(target, "残高が " + sym + " " + amount + " に設定されました。");
            }
            default -> sender.sendMessage("使用方法: /eco <give|take|set> <player> <amount>");
        }
        return true;
    }
}
