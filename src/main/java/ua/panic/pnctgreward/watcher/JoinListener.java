package ua.panic.pnctgreward.watcher;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.util.MM;

import java.util.UUID;

public class JoinListener implements Listener {
    private final PluginConfig cfg; private final Storage storage;
    public JoinListener(PluginConfig cfg, Storage storage) { this.cfg = cfg; this.storage = storage; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID u = p.getUniqueId();
        storage.popPending(u).thenAccept(optName -> {
            if (optName.isEmpty()) return;
            String name = optName.get();
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("pncTGReward"), () -> {
                for (String cmd : cfg.rewardCmds) {
                    String real = cmd.replace("[player]", name).replace("<name>", name);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
                }
                if (cfg.titleEnabled) MM.title(p, cfg.titleMain, cfg.titleSub, cfg.titleFi, cfg.titleSt, cfg.titleFo);
                if (cfg.soundEnabled) p.playSound(p.getLocation(), cfg.sound, cfg.soundVolume, cfg.soundPitch);
                MM.send(p, cfg.msgPrefix + cfg.msgSuccess);
            });
        });
    }
}
