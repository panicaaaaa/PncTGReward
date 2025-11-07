package ua.panic.pnctgreward.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MM {
    private static final MiniMessage MM;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    static {
        MiniMessage mm;
        try { mm = MiniMessage.miniMessage(); }
        catch (Throwable t) { mm = MiniMessage.builder().build(); }
        MM = mm;
    }

    public static void send(CommandSender s, String raw) {
        String legacy = LEGACY.serialize(MM.deserialize(raw == null ? "" : raw));
        s.sendMessage(legacy);
    }

    public static void title(Player p, String title, String sub, int fi, int st, int fo) {
        String t = LEGACY.serialize(MM.deserialize(title == null ? "" : title));
        String s = LEGACY.serialize(MM.deserialize(sub == null ? "" : sub));
        p.sendTitle(t, s, fi, st, fo);
    }
}
