package world.elyona.economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.event.ElyonaPayEvent;
import world.elyona.core.mimic.MimicMessenger;

public class PayCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final EconomyCache cache;
    private final MimicMessenger mimic;
    private final EconomyConfig config;

    public PayCommand(JavaPlugin plugin, EconomyCache cache, MimicMessenger mimic, EconomyConfig config) {
        this.plugin = plugin;
        this.cache = cache;
        this.mimic = mimic;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        if (args.length < 2) {
            mimic.sendTo(player, "使用方法: /pay <プレイヤー> <金額>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            mimic.sendTo(player, "プレイヤーが見つかりません: " + args[0]);
            return true;
        }

        if (target.equals(player)) {
            mimic.sendTo(player, "自分自身には送金できません。");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            mimic.sendTo(player, "金額は正の整数で指定してください。");
            return true;
        }

        if (amount <= 0) {
            mimic.sendTo(player, "金額は1以上を指定してください。");
            return true;
        }

        // 税計算
        long tax = (long) Math.ceil(amount * config.getTransactionTax());
        long totalDeduct = amount + tax;

        long senderBalance = cache.getBalance(player.getUniqueId());
        if (senderBalance < totalDeduct) {
            mimic.sendTo(player, "残高不足です。必要: " + totalDeduct + " Cr (送金額 " + amount + " + 税 " + tax + "), 現在: " + senderBalance + " Cr");
            return true;
        }

        // 残高操作
        cache.subtractBalance(player.getUniqueId(), totalDeduct);
        cache.addBalance(target.getUniqueId(), amount);
        cache.addServerBalance(tax);

        // イベント発火
        ElyonaPayEvent event = new ElyonaPayEvent(player, target, amount, tax);
        Bukkit.getPluginManager().callEvent(event);

        // 通知
        String sym = config.getCurrencySymbol();
        mimic.sendTo(player, "取引税(" + tax + " Cr)を徴収しました。" + sym + " " + amount + " → " + target.getName() + ": " + sym + " " + (senderBalance - totalDeduct) + " Cr");
        mimic.sendTo(target, player.getName() + "から " + sym + " " + amount + " を受け取りました。");

        plugin.getLogger().info("[Pay] " + player.getName() + " -> " + target.getName() + " " + amount + " Cr (税: " + tax + " Cr)");
        return true;
    }
}
