package world.elyona.economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.event.ElyonaPayEvent;
import world.elyona.core.mimic.MimicMessenger;

import java.util.List;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    /** これを超える金額は税計算や合計額の計算でlongをオーバーフローさせうるため上限とする */
    private static final long MAX_TRANSFER_AMOUNT = Long.MAX_VALUE / 4;

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

        if (amount > MAX_TRANSFER_AMOUNT) {
            mimic.sendTo(player, "金額が大きすぎます。");
            return true;
        }

        // 税計算(税込み合計額がlongの範囲を超える場合は例外にして弾く)
        long tax = (long) Math.ceil(amount * config.getTransactionTax());
        long totalDeduct;
        try {
            totalDeduct = Math.addExact(amount, tax);
        } catch (ArithmeticException e) {
            mimic.sendTo(player, "金額が大きすぎます。");
            return true;
        }

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of(); // 第2引数(金額)は数値のため候補を出さない

        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equals(sender.getName()))
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }
}
