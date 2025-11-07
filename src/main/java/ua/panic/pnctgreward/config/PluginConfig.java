package ua.panic.pnctgreward.config;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    public enum Driver { SQLITE, MARIADB, MYSQL }

    public final Driver driver;
    public final String sqliteFile;
    public final String sqlHost, sqlDb, sqlUser, sqlPass;
    public final int sqlPort;

    public final String tgToken;
    public final String campaignId;
    public final int rateLimitSec;

    public final boolean vaultOn, ppOn;

    public final int rewardVault;
    public final int rewardPP;
    public final java.util.List<String> rewardCmds;

    public final String msgPrefix, msgAlreadyLinked, msgNameAlready, msgClaimed, msgPNotFound, msgSuccess, msgConsoleLog;

    public final boolean titleEnabled; public final String titleMain, titleSub; public final int titleFi, titleSt, titleFo;

    public final boolean soundEnabled; public final Sound sound; public final float soundVolume; public final float soundPitch;

    public PluginConfig(FileConfiguration c) {
        this.driver = Driver.valueOf(c.getString("Database.driver", "sqlite").toUpperCase());
        this.sqliteFile = c.getString("Database.sqlite.file", "plugins/pncTGReward/data.db");
        this.sqlHost = c.getString("Database."+driver.name().toLowerCase()+".host", "127.0.0.1");
        this.sqlPort = c.getInt("Database."+driver.name().toLowerCase()+".port", 3306);
        this.sqlDb = c.getString("Database."+driver.name().toLowerCase()+".database", "tgreward");
        this.sqlUser = c.getString("Database."+driver.name().toLowerCase()+".user", "root");
        this.sqlPass = c.getString("Database."+driver.name().toLowerCase()+".password", "");

        this.tgToken = c.getString("Telegram.token", "");
        this.campaignId = c.getString("Telegram.campaign-id", "default");
        this.rateLimitSec = c.getInt("Telegram.rate-limit-seconds", 3);

        this.vaultOn = c.getBoolean("Economic.Vault", true);
        this.ppOn = c.getBoolean("Economic.PlayerPoints", true);

        this.rewardVault = c.getInt("Reward.Vault", 0);
        this.rewardPP = c.getInt("Reward.PlayerPoints", 0);
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
        try { tmp = Sound.valueOf(soundName.toUpperCase()); } catch (IllegalArgumentException e) { tmp = Sound.ENTITY_PLAYER_LEVELUP; }
        this.sound = tmp;
        this.soundVolume = (float) c.getDouble("Sound.volume", 1.0D);
        this.soundPitch  = (float) c.getDouble("Sound.pitch", 1.0D);
    }
}
