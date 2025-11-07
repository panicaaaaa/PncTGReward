package ua.panic.pnctgreward.economy;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ua.panic.pnctgreward.config.PluginConfig;

import java.util.UUID;

public class EconomyBridge {
    private final JavaPlugin plugin; private final PluginConfig cfg;
    private Economy vault; private PlayerPointsAPI pp;
    public EconomyBridge(JavaPlugin plugin, PluginConfig cfg) { this.plugin = plugin; this.cfg = cfg; }
    public void hook() {
        if (cfg.vaultOn) { RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class); if (rsp != null) vault = rsp.getProvider(); }
        if (cfg.ppOn && Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) { pp = PlayerPoints.getInstance().getAPI(); }
    }
    public void unhook() { }
    public void pay(UUID uuid, int vaultMoney, int points) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (cfg.vaultOn && vault != null && vaultMoney > 0) vault.depositPlayer(op, vaultMoney);
        if (cfg.ppOn && pp != null && points > 0) pp.give(uuid, points);
    }
}