package world.elyona.economy;

import world.elyona.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyCache {

    private final DatabaseManager db;
    /** オンラインプレイヤーの残高キャッシュ */
    private final Map<UUID, Long> cache = new ConcurrentHashMap<>();

    public EconomyCache(DatabaseManager db) {
        this.db = db;
    }

    /** ログイン時: DBから残高をロードしてキャッシュに入れる */
    public CompletableFuture<Long> load(UUID uuid) {
        return db.queryAsync(
                "SELECT balance FROM elyona_economy WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString()),
                rs -> {
                    long bal = rs.next() ? rs.getLong("balance") : 0L;
                    cache.put(uuid, bal);
                    return bal;
                }
        );
    }

    /** ログアウト時: キャッシュの残高をDBに書き込む */
    public void flush(UUID uuid) {
        Long balance = cache.remove(uuid);
        if (balance == null) return;
        db.executeSync(
                "UPDATE elyona_economy SET balance = ? WHERE uuid = ?",
                ps -> {
                    ps.setLong(1, balance);
                    ps.setString(2, uuid.toString());
                }
        );
    }

    /** onDisable: 全オンラインプレイヤーの残高を同期で保存 */
    public void flushAll() {
        for (Map.Entry<UUID, Long> entry : cache.entrySet()) {
            db.executeSync(
                    "UPDATE elyona_economy SET balance = ? WHERE uuid = ?",
                    ps -> {
                        ps.setLong(1, entry.getValue());
                        ps.setString(2, entry.getKey().toString());
                    }
            );
        }
        cache.clear();
    }

    /** 残高取得（キャッシュ優先、なければDB同期クエリ） */
    public long getBalance(UUID uuid) {
        Long cached = cache.get(uuid);
        if (cached != null) return cached;
        // オフラインプレイヤー向けDB同期クエリ（/eco操作などでオフラインUUIDを直接指定するケースに対応）
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT balance FROM elyona_economy WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("balance") : 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 残高設定（キャッシュ更新と同時にDBへも即時反映する） */
    public void setBalance(UUID uuid, long amount) {
        long clamped = Math.max(0, amount);
        cache.put(uuid, clamped);
        persist(uuid, clamped);
    }

    /**
     * 残高加算（キャッシュ更新と同時にDBへも即時反映する）。
     * amountが負の場合、符号反転により実質的な減算になりsubtractBalanceの残高チェックを
     * すり抜けてしまうため、呼び出し元のバグとして早期に検出できるよう例外にする。
     */
    public void addBalance(UUID uuid, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("addBalanceに負の値は渡せません: " + amount);
        }
        long newBalance = cache.merge(uuid, amount, Long::sum);
        persist(uuid, newBalance);
    }

    /**
     * 残高減算（キャッシュ更新と同時にDBへも即時反映する）。
     * amountが負の場合、current < amount の判定が意図と逆転し実質的な加算になってしまうため、
     * 呼び出し元のバグとして早期に検出できるよう例外にする。
     */
    public boolean subtractBalance(UUID uuid, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("subtractBalanceに負の値は渡せません: " + amount);
        }
        Long current = cache.get(uuid);
        if (current == null) current = getBalance(uuid);
        if (current < amount) return false;
        long newBalance = current - amount;
        cache.put(uuid, newBalance);
        persist(uuid, newBalance);
        return true;
    }

    /** 残高をDBへ非同期で即時書き込む（サーバー強制終了・クラッシュ時のデータロスを防ぐ） */
    private void persist(UUID uuid, long balance) {
        db.executeAsync(
                "UPDATE elyona_economy SET balance = ? WHERE uuid = ?",
                ps -> {
                    ps.setLong(1, balance);
                    ps.setString(2, uuid.toString());
                }
        );
    }

    /** キャッシュにロード済みか（= オンライン） */
    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /** サーバー口座のUUID */
    public static UUID getServerUuid() {
        return EconomyRepository.SERVER_UUID;
    }

    /** サーバー口座残高（DBから都度取得） */
    public long getServerBalance() {
        return getBalance(EconomyRepository.SERVER_UUID);
    }

    /** サーバー口座に入金（DB直接更新） */
    public void addServerBalance(long amount) {
        db.executeSync(
                "UPDATE elyona_economy SET balance = balance + ? WHERE uuid = ?",
                ps -> {
                    ps.setLong(1, amount);
                    ps.setString(2, EconomyRepository.SERVER_UUID.toString());
                }
        );
    }
}
