package ua.panic.pnctgreward.config;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PluginConfig {
    public enum Driver { SQLITE, MARIADB, MYSQL }
    public final Driver driver;
    public final String sqliteFile;
    public final String sqlHost, sqlDb, sqlUser, sqlPass, sqlParams;
    public final int sqlPort;

    public final String tgToken;
    public final String campaignId;
    public final int rateLimitSec;

    public final boolean rewardOneTime;
    public final long rewardCooldownSec;
    public final List<String> rewardCmds;

    public final String msgPrefix, msgAlreadyLinked, msgNameAlready, msgClaimed, msgPNotFound, msgSuccess, msgConsoleLog;

    public final boolean titleEnabled; public final String titleMain, titleSub; public final int titleFi, titleSt, titleFo;
    public final boolean soundEnabled; public final Sound sound; public final float soundVolume; public final float soundPitch;

    public final List<String> tgMsgUsage;
    public final List<String> tgMsgRateLimited;
    public final List<String> tgMsgClaimedAlready;
    public final List<String> tgMsgPlayerNotFound;
    public final List<String> tgMsgNameAlreadyLinked;
    public final List<String> tgMsgTgAlreadyLinked;
    public final List<String> tgMsgRewardSuccess;
    public final List<String> tgMsgCooldown;

    public final String timeOne, timeTwo, timeThree, timeFour;

    public final String phStatusYes, phStatusNo, phCooldownYes, phCooldownNo;

    public PluginConfig(FileConfiguration c) {
        this.driver = Driver.valueOf(c.getString("Database.driver", "sqlite").toUpperCase());
        this.sqliteFile = c.getString("Database.sqlite.file", "plugins/pncTGReward/data.db");
        String k = driver.name().toLowerCase();
        this.sqlHost = c.getString("Database."+k+".host", "127.0.0.1");
        this.sqlPort = c.getInt("Database."+k+".port", 3306);
        this.sqlDb = c.getString("Database."+k+".database", "tgreward");
        this.sqlUser = c.getString("Database."+k+".user", "root");
        this.sqlPass = c.getString("Database."+k+".password", "");
        this.sqlParams = c.getString("Database."+k+".params", "useUnicode=true&characterEncoding=utf8&useSSL=false");

        this.tgToken = c.getString("Telegram.token", "");
        this.campaignId = c.getString("Telegram.campaign-id", "default");
        this.rateLimitSec = c.getInt("Telegram.rate-limit-seconds", 3);

        this.rewardOneTime = c.getBoolean("Reward.one-time", true);
        this.rewardCooldownSec = c.getLong("Reward.cooldown", 0L);
        this.rewardCmds = c.getStringList("Reward.Commands");

        this.msgPrefix = c.getString("Messages.prefix", "");
        this.msgAlreadyLinked = c.getString("Messages.already-linked", "");
        this.msgNameAlready = c.getString("Messages.name-already-linked", "");
        this.msgClaimed = c.getString("Messages.claimed-already", "");
        this.msgPNotFound = c.getString("Messages.player-not-found", "");
        this.msgSuccess = c.getString("Messages.success-chat", "");
        this.msgConsoleLog = c.getString("Messages.console-claim-log", "");

        this.titleEnabled = c.getBoolean("Title.enabled", true);
        this.titleMain = c.getString("Title.main", "");
        this.titleSub = c.getString("Title.sub", "");
        this.titleFi = c.getInt("Title.fadein", 10);
        this.titleSt = c.getInt("Title.stay", 50);
        this.titleFo = c.getInt("Title.fadeout", 10);

        this.soundEnabled = c.getBoolean("Sound.enabled", true);
        String soundName = c.getString("Sound.name", "ENTITY_PLAYER_LEVELUP");
        Sound tmp;
        try { tmp = Sound.valueOf(soundName.toUpperCase()); } catch (Exception e) { tmp = Sound.ENTITY_PLAYER_LEVELUP; }
        this.sound = tmp;
        this.soundVolume = (float) c.getDouble("Sound.volume", 1.0D);
        this.soundPitch  = (float) c.getDouble("Sound.pitch", 1.0D);

        this.tgMsgUsage = c.getStringList("Messages.telegram.usage");
        this.tgMsgRateLimited = c.getStringList("Messages.telegram.ratelimited");
        this.tgMsgClaimedAlready = c.getStringList("Messages.telegram.claimed-already");
        this.tgMsgPlayerNotFound = c.getStringList("Messages.telegram.player-not-found");
        this.tgMsgNameAlreadyLinked = c.getStringList("Messages.telegram.name-already-linked");
        this.tgMsgTgAlreadyLinked = c.getStringList("Messages.telegram.tg-already-linked");
        this.tgMsgRewardSuccess = c.getStringList("Messages.telegram.reward-success");
        this.tgMsgCooldown = c.getStringList("Messages.telegram.cooldown");

        this.timeOne = c.getString("formating_time.time-one", "<gray>%s д. %s ч. %s м. %s с.</gray>");
        this.timeTwo = c.getString("formating_time.time-two", "<gray>%s ч. %s м. %s с.</gray>");
        this.timeThree = c.getString("formating_time.time-three", "<gray>%s м. %s с.</gray>");
        this.timeFour = c.getString("formating_time.time-four", "<gray>%s с.</gray>");

        this.phStatusYes = c.getString("placeholders.status.yes", "<green>Привязано</green>");
        this.phStatusNo = c.getString("placeholders.status.no", "<red>Не привязано</red>");
        this.phCooldownYes = c.getString("placeholders.cooldown.yes", "<yellow><time></yellow>");
        this.phCooldownNo = c.getString("placeholders.cooldown.no", "<green>Вы можете забрать награду</green>");
    }

    public String timeOne() { return timeOne; }
    public String timeTwo() { return timeTwo; }
    public String timeThree() { return timeThree; }
    public String timeFour() { return timeFour; }
}
