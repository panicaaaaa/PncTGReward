package ua.panic.pnctgreward;

import org.bukkit.plugin.java.JavaPlugin;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.db.jdbc.JdbcStorage;
import ua.panic.pnctgreward.economy.EconomyBridge;
import ua.panic.pnctgreward.telegram.TelegramService;

public final class PncTGReward extends JavaPlugin {
    private PluginConfig cfg;
    private Storage storage;
    private EconomyBridge eco;
    private TelegramService tg;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.cfg = new PluginConfig(getConfig());
        this.storage = new JdbcStorage(this, cfg);
        this.storage.init();
        this.eco = new EconomyBridge(this, cfg);
        this.eco.hook();
        this.tg = new TelegramService(this, cfg, storage, eco);
        this.tg.start();
        getCommand("pnctgr").setExecutor(new ua.panic.pnctgreward.web.AdminCommand(this, storage));
    }

    @Override
    public void onDisable() {
        if (tg != null) tg.shutdown();
        if (eco != null) eco.unhook();
        if (storage != null) storage.close();
    }
}
