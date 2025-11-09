package ua.panic.pnctgreward;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.db.jdbc.JdbcStorage;
import ua.panic.pnctgreward.placeholder.placeholders;
import ua.panic.pnctgreward.telegram.TelegramService;
import ua.panic.pnctgreward.web.AdminCommand;

public final class PncTGReward extends JavaPlugin {
    private PluginConfig cfg;
    private Storage storage;
    private TelegramService tg;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = new PluginConfig(getConfig());
        storage = new JdbcStorage(this, cfg);
        storage.init();
        tg = new TelegramService(this, cfg, storage);
        tg.start();
        getCommand("pnctgr").setExecutor(new AdminCommand(this, storage));
        Bukkit.getPluginManager().registerEvents(new ua.panic.pnctgreward.watcher.JoinListener(cfg, storage), this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new placeholders(this, cfg, storage).register();
    }

    @Override
    public void onDisable() {
        if (tg != null) tg.shutdown();
        if (storage != null) storage.close();
    }

    public PluginConfig cfg() { return cfg; }
    public Storage storage() { return storage; }
}
