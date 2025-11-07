package ua.panic.pnctgreward.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.panic.pnctgreward.config.PluginConfig;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.economy.EconomyBridge;
import ua.panic.pnctgreward.util.MM;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramService {
    private final JavaPlugin plugin;
    private final PluginConfig cfg;
    private final Storage storage;
    private final EconomyBridge eco;

    private TelegramBot bot;
    private final Map<Long, Long> ratelimit = new ConcurrentHashMap<>();
    private final String tgUsage;
    private final String tgRateLimited;
    private final String tgClaimedAlready;
    private final String tgPlayerNotFound;
    private final String tgNameAlreadyLinked;
    private final String tgTgAlreadyLinked;
    private final String tgRewardSuccess;

    public TelegramService(JavaPlugin plugin, PluginConfig cfg, Storage storage, EconomyBridge eco) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.storage = storage;
        this.eco = eco;

        var c = plugin.getConfig();
        this.tgUsage            = c.getString("Messages.telegram.usage", "Использование: /reward <ник>");
        this.tgRateLimited      = c.getString("Messages.telegram.ratelimited", "Слишком часто. Подожди пару секунд и попробуй снова.");
        this.tgClaimedAlready   = c.getString("Messages.telegram.claimed-already", "❌ Уже получали награду по этой кампании.");
        this.tgPlayerNotFound   = c.getString("Messages.telegram.player-not-found", "❌ Игрок не найден: <name>");
        this.tgNameAlreadyLinked= c.getString("Messages.telegram.name-already-linked", "❌ Этот ник уже привязан к другому Telegram.");
        this.tgTgAlreadyLinked  = c.getString("Messages.telegram.tg-already-linked", "❌ Этот Telegram уже привязан к другому нику.");
        this.tgRewardSuccess    = c.getString("Messages.telegram.reward-success", "✅ Награда выдана игроку <name>");
    }

    public void start() {
        if (cfg.tgToken == null || cfg.tgToken.isBlank()) {
            plugin.getLogger().warning("Telegram token is empty — bot disabled");
            return;
        }
        bot = new TelegramBot(cfg.tgToken);
        bot.setUpdatesListener(updates -> {
            for (Update u : updates) handleUpdate(u);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        plugin.getLogger().info("Telegram bot started");
    }

    public void shutdown() {
        if (bot != null) bot.shutdown();
    }

    private void handleUpdate(Update u) {
        Message m = u.message();
        if (m == null || m.text() == null) return;

        long tgId = m.from().id();

        if (cfg.rateLimitSec > 0) {
            long now = System.currentTimeMillis();
            long last = ratelimit.getOrDefault(tgId, 0L);
            if (now - last < cfg.rateLimitSec * 1000L) {
                bot.execute(new SendMessage(m.chat().id(), tgRateLimited));
                return;
            }
            ratelimit.put(tgId, now);
        }

        String[] parts = m.text().trim().split("\\s+");
        if (parts.length == 0) return;

        if (parts[0].equalsIgnoreCase("/reward")) {
            if (parts.length < 2) {
                bot.execute(new SendMessage(m.chat().id(), tgUsage));
                return;
            }
            String name = parts[1];
            processReward(tgId, name, m.chat().id());
        }
    }

    private void processReward(long tgId, String name, long chatId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (storage.hasClaim(tgId, cfg.campaignId).join()) {
                bot.execute(new SendMessage(chatId, tgClaimedAlready));
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            UUID uuid = op.getUniqueId();
            if (uuid == null) {
                bot.execute(new SendMessage(chatId, tgPlayerNotFound.replace("<name>", name)));
                return;
            }

            var ok = storage.link(tgId, uuid, name).join();
            if (!ok) {
                var other = storage.getTgByUuid(uuid).join();
                if (other.isPresent() && other.get() != tgId) {
                    bot.execute(new SendMessage(chatId, tgNameAlreadyLinked));
                } else {
                    bot.execute(new SendMessage(chatId, tgTgAlreadyLinked));
                }
                return;
            }

            boolean first = storage.markClaim(tgId, cfg.campaignId).join();
            if (!first) {
                bot.execute(new SendMessage(chatId, tgClaimedAlready));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                eco.pay(uuid, cfg.rewardVault, cfg.rewardPP);

                for (String cmd : cfg.rewardCmds) {
                    String real = cmd.replace("[player]", name).replace("<name>", name);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
                }

                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    if (cfg.titleEnabled) {
                        MM.title(p, cfg.titleMain, cfg.titleSub, cfg.titleFi, cfg.titleSt, cfg.titleFo);
                    }
                    if (cfg.soundEnabled) {
                        p.playSound(p.getLocation(), cfg.sound, cfg.soundVolume, cfg.soundPitch);
                    }
                    MM.send(p, cfg.msgPrefix + cfg.msgSuccess);
                }
            });

            bot.execute(new SendMessage(chatId, tgRewardSuccess.replace("<name>", name)));
        });
    }
}
