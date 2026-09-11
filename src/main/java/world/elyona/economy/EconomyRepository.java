package world.elyona.economy;

import world.elyona.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * elyona_economy テーブルの所有者。
 * 初回参加ボーナス済みフラグ(initial_grant_done)もここで管理する
 * （旧ElyonaCoreのelyona_playersから移行。経済ドメインの状態のため）。
 */
public class EconomyRepository {

    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final DatabaseManager db;

    public EconomyRepository(DatabaseManager db) {
        this.db = db;
    }

    /** elyona_economy テーブルを作成し、サーバー口座を確保する（onEnable時に同期実行） */
    public void initialize() {
        boolean mysql = db.isMySql();
        String boolType = mysql ? "TINYINT(1)" : "BOOLEAN";
        String sql = "CREATE TABLE IF NOT EXISTS elyona_economy (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "balance BIGINT DEFAULT 0," +
                "initial_grant_done " + boolType + " DEFAULT 0" +
                ")";

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("elyona_economy テーブル作成に失敗しました", e);
        }

        String insertServer = mysql
                ? "INSERT IGNORE INTO elyona_economy (uuid, balance, initial_grant_done) VALUES (?, 0, 1)"
                : "INSERT OR IGNORE INTO elyona_economy (uuid, balance, initial_grant_done) VALUES (?, 0, 1)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertServer)) {
            ps.setString(1, SERVER_UUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("サーバー口座の作成に失敗しました", e);
        }
    }

    /** 口座レコードが無ければ作成する（初回参加時） */
    public CompletableFuture<Void> ensureAccount(UUID uuid) {
        String sql = db.isMySql()
                ? "INSERT IGNORE INTO elyona_economy (uuid, balance, initial_grant_done) VALUES (?, 0, 0)"
                : "INSERT OR IGNORE INTO elyona_economy (uuid, balance, initial_grant_done) VALUES (?, 0, 0)";
        return db.executeAsync(sql, ps -> ps.setString(1, uuid.toString()));
    }

    /** 初回参加ボーナス済みかどうかを取得する */
    public CompletableFuture<Boolean> isInitialGrantDone(UUID uuid) {
        return db.queryAsync(
                "SELECT initial_grant_done FROM elyona_economy WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString()),
                rs -> rs.next() && rs.getBoolean("initial_grant_done")
        );
    }

    public CompletableFuture<Void> markInitialGrantDone(UUID uuid) {
        return db.executeAsync(
                "UPDATE elyona_economy SET initial_grant_done = 1 WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString())
        );
    }
}
