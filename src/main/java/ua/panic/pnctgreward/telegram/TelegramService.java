package ua.panic.pnctgreward.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.util.MM;
import ua.panic.pnctgreward.util.TimeFmt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramService {
    private final JavaPlugin plugin;
    private final PluginConfig cfg;
    private final Storage storage;
    private TelegramBot bot;
    private final Map<Long, Long> ratelimit = new ConcurrentHashMap<>();

    public TelegramService(JavaPlugin plugin, PluginConfig cfg, Storage storage) {
        this.plugin = plugin; this.cfg = cfg; this.storage = storage;
    }

    public void start() {
        if (cfg.tgToken == null || cfg.tgToken.isBlank()) { plugin.getLogger().warning("Telegram token is empty — bot disabled"); return; }
        bot = new TelegramBot(cfg.tgToken);
        bot.setUpdatesListener(updates -> { for (Update u : updates) handleUpdate(u); return UpdatesListener.CONFIRMED_UPDATES_ALL; });
    }

    public void shutdown() { if (bot != null) bot.shutdown(); }

    private void handleUpdate(Update u) {
        Message m = u.message();
        if (m == null || m.text() == null) return;
        long tgId = m.from().id();
        if (cfg.rateLimitSec > 0) {
            long now = System.currentTimeMillis();
            long last = ratelimit.getOrDefault(tgId, 0L);
            if (now - last < cfg.rateLimitSec * 1000L) { sendMd(m.chat().id(), join(cfg.tgMsgRateLimited)); return; }
            ratelimit.put(tgId, now);
        }
        String[] parts = m.text().trim().split("\\s+");
        if (parts.length == 0) return;
        if (parts[0].equalsIgnoreCase("/reward") || parts[0].startsWith("/reward@")) {
            if (parts.length < 2) { sendMd(m.chat().id(), join(cfg.tgMsgUsage)); return; }
            String name = parts[1];
            processReward(tgId, name, m.chat().id());
        }
    }

    private void processReward(long tgId, String name, long chatId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (cfg.rewardOneTime && storage.hasAnyClaim(tgId, cfg.campaignId).join()) { sendMd(chatId, join(cfg.tgMsgClaimedAlready)); return; }
            long now = Instant.now().getEpochSecond();
            if (!cfg.rewardOneTime && cfg.rewardCooldownSec > 0) {
                long last = storage.getLastClaimEpoch(tgId, cfg.campaignId).join();
                if (last > 0) {
                    long left = cfg.rewardCooldownSec - (now - last);
                    if (left > 0) {
                        String t = TimeFmt.format(left, cfg);
                        sendMd(chatId, join(cfg.tgMsgCooldown).replace("<time>", escapeMd(t)));
                        return;
                    }
                }
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            UUID uuid = op.getUniqueId();
            if (uuid == null) { sendMd(chatId, join(cfg.tgMsgPlayerNotFound).replace("<name>", escapeMd(name))); return; }
            var ok = storage.link(tgId, uuid, name).join();
            if (!ok) {
                var other = storage.getTgByUuid(uuid).join();
                if (other.isPresent() && other.get() != tgId) sendMd(chatId, join(cfg.tgMsgNameAlreadyLinked));
                else sendMd(chatId, join(cfg.tgMsgTgAlreadyLinked));
                return;
            }
            storage.recordClaim(tgId, cfg.campaignId, now).join();
            if (op.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = op.getPlayer();
                    if (p != null) {
                        for (String cmd : cfg.rewardCmds) {
                            String real = cmd.replace("[player]", name).replace("<name>", name);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
                        }
                        if (cfg.titleEnabled) MM.title(p, cfg.titleMain, cfg.titleSub, cfg.titleFi, cfg.titleSt, cfg.titleFo);
                        if (cfg.soundEnabled) p.playSound(p.getLocation(), cfg.sound, cfg.soundVolume, cfg.soundPitch);
                        MM.send(p, cfg.msgPrefix + cfg.msgSuccess);
                    }
                });
                sendMd(chatId, join(cfg.tgMsgRewardSuccess).replace("<name>", escapeMd(name)));
            } else {
                storage.upsertPending(uuid, name, cfg.campaignId).join();
                sendMd(chatId, join(cfg.tgMsgRewardSuccess).replace("<name>", escapeMd(name)));
            }
        });
    }

    private void sendMd(long chatId, String text) {
        bot.execute(
                new SendMessage(chatId, text)
                        .parseMode(ParseMode.MarkdownV2)
                        .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true)) // 👈 вместо disableWebPagePreview
        );
    }

    private static String join(java.util.List<String> lines) { return String.join("\n", lines == null ? java.util.List.of("") : lines); }

    private static String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("_","\\_").replace("*","\\*").replace("[","\\[").replace("]","\\]").replace("(","\\(").replace(")","\\)")
                .replace("~","\\~").replace("`","\\`").replace(">","\\>").replace("#","\\#").replace("+","\\+").replace("-","\\-")
                .replace("=","\\=").replace("|","\\|").replace("{","\\{").replace("}","\\}").replace(".","\\.").replace("!","\\!");
    }

    private static String escapeMdRaw(String s) { return escapeMd(s); }
}
