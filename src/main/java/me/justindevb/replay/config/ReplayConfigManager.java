package me.justindevb.replay.config;

import me.justindevb.replay.Replay;

import java.io.File;

public class ReplayConfigManager {

    private static final int CURRENT_CONFIG_VERSION = 2;

    private static final String[] HEADER = new String[] {
            "===========================================",
            "        BetterReplay Configuration",
            "==========================================="
    };

    private final Replay plugin;

    public ReplayConfigManager(Replay plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        boolean existed = configFile.exists();

        CommentedFileConfiguration commented = new CommentedFileConfiguration(plugin, configFile);
        commented.load();
        int currentVersion = commented.getInt(ReplayConfigSetting.CONFIG_VERSION.getKey(), 0);
        boolean needsCommentBackfill = existed && currentVersion < CURRENT_CONFIG_VERSION;

        boolean changed = false;
        if (!existed) {
            commented.addHeaderComments(HEADER);
            changed = true;
        } else if (needsCommentBackfill) {
            changed |= commented.addHeaderCommentsIfMissing(HEADER);
        }

        for (ReplayConfigSetting setting : ReplayConfigSetting.values()) {
            changed |= commented.setIfNotExists(setting);
            if (needsCommentBackfill) {
                changed |= commented.ensureSettingComments(setting);
            }
        }

        changed |= commented.setIfDifferent(ReplayConfigSetting.CONFIG_VERSION.getKey(), CURRENT_CONFIG_VERSION);

        if (changed) {
            commented.save();
        }

        plugin.reloadConfig();
    }
}
