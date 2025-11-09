package ua.panic.pnctgreward.util;

import ua.panic.pnctgreward.config.PluginConfig;

public final class TimeFmt {
    public static String format(long seconds, PluginConfig cfg) {
        long d = seconds / 86400; seconds %= 86400;
        long h = seconds / 3600; seconds %= 3600;
        long m = seconds / 60; long s = seconds % 60;
        if (d > 0) return String.format(cfg.timeOne(), d, h, m, s);
        if (h > 0) return String.format(cfg.timeTwo(), h, m, s);
        if (m > 0) return String.format(cfg.timeThree(), m, s);
        return String.format(cfg.timeFour(), s);
    }
}
