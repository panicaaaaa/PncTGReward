package ua.panic.pnctgreward.db.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JdbcStorage implements Storage {
    private final Plugin plugin;
    private final PluginConfig cfg;
    private HikariDataSource ds;

    public JdbcStorage(Plugin plugin, PluginConfig cfg) { this.plugin = plugin; this.cfg = cfg; }

    @Override public void init() {
        HikariConfig hc = new HikariConfig();
        switch (cfg.driver) {
            case SQLITE -> { hc.setJdbcUrl("jdbc:sqlite:" + cfg.sqliteFile); hc.setDriverClassName("org.sqlite.JDBC"); }
            case MARIADB -> hc.setJdbcUrl("jdbc:mariadb://"+cfg.sqlHost+":"+cfg.sqlPort+"/"+cfg.sqlDb+"?"+cfg.sqlParams);
            case MYSQL -> hc.setJdbcUrl("jdbc:mysql://"+cfg.sqlHost+":"+cfg.sqlPort+"/"+cfg.sqlDb+"?"+cfg.sqlParams);
        }
        if (cfg.driver != PluginConfig.Driver.SQLITE) { hc.setUsername(cfg.sqlUser); hc.setPassword(cfg.sqlPass); }
        hc.setMaximumPoolSize(5);
        ds = new HikariDataSource(hc);
        runDDL();
    }

    private void runDDL() {
        String accounts = "CREATE TABLE IF NOT EXISTS accounts (" +
                "telegram_id BIGINT PRIMARY KEY," +
                "mc_uuid CHAR(36) UNIQUE NOT NULL," +
                "mc_name VARCHAR(16) NOT NULL," +
                "linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        String claims = "CREATE TABLE IF NOT EXISTS claims (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "telegram_id BIGINT NOT NULL," +
                "campaign_id VARCHAR(64) NOT NULL," +
                "claimed_at BIGINT NOT NULL)";
        if (cfg.driver != PluginConfig.Driver.SQLITE) claims = "CREATE TABLE IF NOT EXISTS claims (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "telegram_id BIGINT NOT NULL," +
                "campaign_id VARCHAR(64) NOT NULL," +
                "claimed_at BIGINT NOT NULL)";
        String pending = "CREATE TABLE IF NOT EXISTS pending (" +
                "mc_uuid CHAR(36) PRIMARY KEY," +
                "mc_name VARCHAR(16) NOT NULL," +
                "campaign_id VARCHAR(64) NOT NULL," +
                "created_at BIGINT NOT NULL)";
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(accounts);
            st.execute(claims);
            st.execute(pending);
        } catch (Exception e) { plugin.getLogger().severe("DB DDL error: " + e.getMessage()); }
    }

    @Override public void close() { if (ds != null) ds.close(); }

    @Override public CompletableFuture<Boolean> link(long tgId, UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = switch (cfg.driver) {
                case SQLITE -> "INSERT INTO accounts(telegram_id, mc_uuid, mc_name) VALUES (?,?,?) " +
                        "ON CONFLICT(telegram_id) DO UPDATE SET mc_uuid=excluded.mc_uuid, mc_name=excluded.mc_name";
                default -> "INSERT INTO accounts(telegram_id, mc_uuid, mc_name) VALUES (?,?,?) " +
                        "ON DUPLICATE KEY UPDATE mc_uuid=VALUES(mc_uuid), mc_name=VALUES(mc_name)";
            };
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                Optional<Long> ex = getTgByUuidSync(uuid);
                if (ex.isPresent() && ex.get() != tgId) return false;
                ps.setLong(1, tgId); ps.setString(2, uuid.toString()); ps.setString(3, name);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    private Optional<Long> getTgByUuidSync(UUID uuid) {
        String q = "SELECT telegram_id FROM accounts WHERE mc_uuid=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(rs.getLong(1)); }
        } catch (SQLException ignored) {}
        return Optional.empty();
    }

    @Override public CompletableFuture<Optional<UUID>> getUuidByTg(long tgId) {
        return CompletableFuture.supplyAsync(() -> {
            String q = "SELECT mc_uuid FROM accounts WHERE telegram_id=?";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setLong(1, tgId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(UUID.fromString(rs.getString(1))); }
            } catch (SQLException ignored) {}
            return Optional.empty();
        });
    }

    @Override public CompletableFuture<Optional<Long>> getTgByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getTgByUuidSync(uuid));
    }

    @Override public CompletableFuture<Long> getLastClaimEpoch(long tgId, String campaignId) {
        return CompletableFuture.supplyAsync(() -> {
            String q = "SELECT MAX(claimed_at) FROM claims WHERE telegram_id=? AND campaign_id=?";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setLong(1, tgId); ps.setString(2, campaignId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong(1); }
            } catch (SQLException ignored) {}
            return 0L;
        });
    }

    @Override public CompletableFuture<Void> recordClaim(long tgId, String campaignId, long epochSeconds) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO claims(telegram_id, campaign_id, claimed_at) VALUES (?,?,?)";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, tgId); ps.setString(2, campaignId); ps.setLong(3, epochSeconds);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    @Override public CompletableFuture<Boolean> hasAnyClaim(long tgId, String campaignId) {
        return CompletableFuture.supplyAsync(() -> {
            String q = "SELECT 1 FROM claims WHERE telegram_id=? AND campaign_id=? LIMIT 1";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setLong(1, tgId); ps.setString(2, campaignId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            } catch (SQLException ignored) {}
            return false;
        });
    }

    @Override public CompletableFuture<Void> upsertPending(UUID uuid, String name, String campaignId) {
        return CompletableFuture.runAsync(() -> {
            long now = Instant.now().getEpochSecond();
            String sql = switch (cfg.driver) {
                case SQLITE -> "INSERT INTO pending(mc_uuid, mc_name, campaign_id, created_at) VALUES (?,?,?,?) " +
                        "ON CONFLICT(mc_uuid) DO UPDATE SET mc_name=excluded.mc_name, campaign_id=excluded.campaign_id, created_at=excluded.created_at";
                default -> "INSERT INTO pending(mc_uuid, mc_name, campaign_id, created_at) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE mc_name=VALUES(mc_name), campaign_id=VALUES(campaign_id), created_at=VALUES(created_at)";
            };
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setString(3, campaignId); ps.setLong(4, now);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    @Override public CompletableFuture<Optional<String>> popPending(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String q = "SELECT mc_name FROM pending WHERE mc_uuid=?";
            String d = "DELETE FROM pending WHERE mc_uuid=?";
            try (Connection c = ds.getConnection()) {
                String name = null;
                try (PreparedStatement ps = c.prepareStatement(q)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) name = rs.getString(1); }
                }
                if (name != null) {
                    try (PreparedStatement ps = c.prepareStatement(d)) { ps.setString(1, uuid.toString()); ps.executeUpdate(); }
                    return Optional.of(name);
                }
            } catch (SQLException ignored) {}
            return Optional.empty();
        });
    }
}
