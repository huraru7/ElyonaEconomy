package world.elyona.economy;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import world.elyona.core.event.ElyonaRankUpEvent;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.core.rank.RankTier;

/**
 * ElyonaRankUpEvent（ElyonaCore/将来のElyonaRankが発火）をリッスンし、
 * ランクアップ報酬のCr付与のみを担当する（称号付与・通知本体はElyonaCore/ElyonaTitle側）。
 */
public class RankRewardListener implements Listener {

    private final EconomyCache cache;
    private final EconomyConfig config;
    private final MimicMessenger mimic;

    public RankRewardListener(EconomyCache cache, EconomyConfig config, MimicMessenger mimic) {
        this.cache = cache;
        this.config = config;
        this.mimic = mimic;
    }

    @EventHandler
    public void onRankUp(ElyonaRankUpEvent event) {
        RankTier newRank = event.getNewRank();
        if (newRank.crReward <= 0) return;

        Player player = event.getPlayer();
        cache.addBalance(player.getUniqueId(), newRank.crReward);
        mimic.sendTo(player, "ランクアップ報酬として " + config.getCurrencySymbol() + " " + newRank.crReward + " を受け取りました！");
    }
}
