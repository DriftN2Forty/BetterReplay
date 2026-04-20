package me.justindevb.replay.config;

import me.justindevb.replay.Replay;

import java.io.File;

public class ReplayConfigManager {

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

        boolean changed = false;
        if (!existed) {
            commented.addHeaderComments(HEADER);
            changed = true;
        }

        for (ReplayConfigSetting setting : ReplayConfigSetting.values()) {
            changed |= commented.setIfNotExists(setting);
        }

        if (changed) {
            commented.save();
        }

        plugin.reloadConfig();
    }
}
