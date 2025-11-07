package ua.panic.pnctgreward.db.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;

import java.sql.*;
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
            case MARIADB -> hc.setJdbcUrl("jdbc:mariadb://"+cfg.sqlHost+":"+cfg.sqlPort+"/"+cfg.sqlDb);
            case MYSQL -> hc.setJdbcUrl("jdbc:mysql://"+cfg.sqlHost+":"+cfg.sqlPort+"/"+cfg.sqlDb);
        }
        if (cfg.driver != PluginConfig.Driver.SQLITE) { hc.setUsername(cfg.sqlUser); hc.setPassword(cfg.sqlPass); }
        hc.setMaximumPoolSize(5);
        ds = new HikariDataSource(hc);
        runDDL();
    }
    private void runDDL() {
        final String accounts = "CREATE TABLE IF NOT EXISTS accounts ("+
                "telegram_id BIGINT PRIMARY KEY,"+
                "mc_uuid CHAR(36) UNIQUE NOT NULL,"+
                "mc_name VARCHAR(16) NOT NULL,"+
                "linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        final String claims = "CREATE TABLE IF NOT EXISTS claims ("+
                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "telegram_id BIGINT NOT NULL, "+
                "campaign_id VARCHAR(64) NOT NULL, "+
                "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "+
                "UNIQUE(telegram_id, campaign_id))";
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) { st.execute(accounts); st.execute(claims); } catch (Exception e) { plugin.getLogger().severe("DB DDL error: "+e.getMessage()); }
    }
    @Override public void close() { if (ds != null) ds.close(); }
    @Override public CompletableFuture<Boolean> link(long tgId, UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = switch (cfg.driver) {
                case SQLITE -> "INSERT INTO accounts(telegram_id, mc_uuid, mc_name) VALUES (?,?,?) ON CONFLICT(telegram_id) DO UPDATE SET mc_uuid=excluded.mc_uuid, mc_name=excluded.mc_name";
                default -> "INSERT INTO accounts(telegram_id, mc_uuid, mc_name) VALUES (?,?,?) ON DUPLICATE KEY UPDATE mc_uuid=VALUES(mc_uuid), mc_name=VALUES(mc_name)";
            };
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                var tgOfUuid = getTgByUuidSync(uuid);
                if (tgOfUuid.isPresent() && tgOfUuid.get() != tgId) return false;
                ps.setLong(1, tgId); ps.setString(2, uuid.toString()); ps.setString(3, name);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }
    private Optional<Long> getTgByUuidSync(UUID uuid) {
        final String q = "SELECT telegram_id FROM accounts WHERE mc_uuid=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(rs.getLong(1)); return Optional.empty(); }
        } catch (SQLException e) { return Optional.empty(); }
    }
    @Override public CompletableFuture<Optional<UUID>> getUuidByTg(long tgId) {
        return CompletableFuture.supplyAsync(() -> {
            final String q = "SELECT mc_uuid FROM accounts WHERE telegram_id=?";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setLong(1, tgId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(UUID.fromString(rs.getString(1))); return Optional.empty(); }
            } catch (SQLException e) { return Optional.empty(); }
        });
    }
    @Override public CompletableFuture<Optional<Long>> getTgByUuid(UUID uuid) { return CompletableFuture.supplyAsync(() -> getTgByUuidSync(uuid)); }
    @Override public CompletableFuture<Boolean> markClaim(long tgId, String campaignId) {
        return CompletableFuture.supplyAsync(() -> {
            final String sql = switch (cfg.driver) {
                case SQLITE -> "INSERT INTO claims(telegram_id, campaign_id) VALUES (?,?) ON CONFLICT(telegram_id, campaign_id) DO NOTHING";
                default -> "INSERT IGNORE INTO claims(telegram_id, campaign_id) VALUES (?,?)";
            };
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, tgId); ps.setString(2, campaignId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }
    @Override public CompletableFuture<Boolean> hasClaim(long tgId, String campaignId) {
        return CompletableFuture.supplyAsync(() -> {
            final String q = "SELECT 1 FROM claims WHERE telegram_id=? AND campaign_id=?";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setLong(1, tgId); ps.setString(2, campaignId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            } catch (SQLException e) { return false; }
        });
    }
}