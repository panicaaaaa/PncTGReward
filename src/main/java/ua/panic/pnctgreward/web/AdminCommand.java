package ua.panic.pnctgreward.web;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ua.panic.pnctgreward.PncTGReward;
import ua.panic.pnctgreward.db.Storage;
import ua.panic.pnctgreward.util.MM;

import java.util.UUID;

public class AdminCommand implements CommandExecutor {
    private final PncTGReward plugin; private final Storage storage;
    public AdminCommand(PncTGReward plugin, Storage storage) { this.plugin = plugin; this.storage = storage; }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("pnctgr.admin")) { MM.send(s, "<red>No permission</red>"); return true; }
        if (args.length == 0) {
            MM.send(s, "<gray>/" + label + " reload</gray>");
            MM.send(s, "<gray>/" + label + " check <tgId|playerName></gray>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                MM.send(s, "<green>Конфиг перезагружен</green>");
            }
            case "check" -> {
                if (args.length < 2) { MM.send(s, "<red>Укажи tgId или ник</red>"); return true; }
                String arg = args[1];
                try {
                    long tg = Long.parseLong(arg);
                    storage.getUuidByTg(tg).thenAccept(opt -> {
                        if (opt.isPresent()) MM.send(s, "✔ tgId=" + tg + " → uuid=" + opt.get());
                        else MM.send(s, "✖ tgId не найден");
                    });
                } catch (NumberFormatException e) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(arg);
                    UUID uuid = op.getUniqueId();
                    if (uuid == null) { MM.send(s, "✖ игрок не найден: " + arg); return true; }
                    storage.getTgByUuid(uuid).thenAccept(opt -> {
                        if (opt.isPresent()) MM.send(s, "✔ name=" + arg + " → tgId=" + opt.get());
                        else MM.send(s, "✖ привязка не найдена для " + arg);
                    });
                }
            }
            default -> MM.send(s, "<yellow>Неизвестная подкоманда</yellow>");
        }
        return true;
    }
}
