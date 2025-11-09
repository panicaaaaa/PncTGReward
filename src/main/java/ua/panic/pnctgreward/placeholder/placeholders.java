package ua.panic.pnctgreward.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ua.panic.pnctgreward.PncTGReward;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.util.TimeFmt;

import java.time.Instant;

public class placeholders extends PlaceholderExpansion {
    private final PncTGReward plugin; private final PluginConfig cfg; private final Storage storage;
    public placeholders(PncTGReward plugin, PluginConfig cfg, Storage storage) { this.plugin = plugin; this.cfg = cfg; this.storage = storage; }
    @Override public String getIdentifier() { return "pncTGReward"; }
    @Override public String getAuthor() { return "panic"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null) return "";
        String key = params.toLowerCase();
        if (key.equals("status_config")) {
            var tg = storage.getTgByUuid(p.getUniqueId()).join();
            return tg.isPresent() ? cfg.phStatusYes : cfg.phStatusNo;
        }
        if (key.equals("status_clean")) {
            var tg = storage.getTgByUuid(p.getUniqueId()).join();
            return tg.isPresent() ? "yes" : "no";
        }
        if (key.equals("cooldown_config")) {
            if (cfg.rewardOneTime) {
                boolean claimed = storage.hasAnyClaim(storage.getTgByUuid(p.getUniqueId()).join().orElse(-1L), cfg.campaignId).join();
                return claimed ? cfg.phCooldownYes.replace("<time>", TimeFmt.format(0, cfg)) : cfg.phCooldownNo;
            }
            var tgOpt = storage.getTgByUuid(p.getUniqueId()).join();
            if (tgOpt.isEmpty()) return cfg.phCooldownNo;
            long now = Instant.now().getEpochSecond();
            long last = storage.getLastClaimEpoch(tgOpt.get(), cfg.campaignId).join();
            long left = cfg.rewardCooldownSec - (now - last);
            if (left > 0) return cfg.phCooldownYes.replace("<time>", TimeFmt.format(left, cfg));
            return cfg.phCooldownNo;
        }
        if (key.equals("cooldown_clean")) {
            if (cfg.rewardOneTime) {
                boolean claimed = storage.hasAnyClaim(storage.getTgByUuid(p.getUniqueId()).join().orElse(-1L), cfg.campaignId).join();
                return claimed ? "yes" : "no";
            }
            var tgOpt = storage.getTgByUuid(p.getUniqueId()).join();
            if (tgOpt.isEmpty()) return "no";
            long now = Instant.now().getEpochSecond();
            long last = storage.getLastClaimEpoch(tgOpt.get(), cfg.campaignId).join();
            long left = cfg.rewardCooldownSec - (now - last);
            return left > 0 ? "yes" : "no";
        }
        return "";
    }
}
