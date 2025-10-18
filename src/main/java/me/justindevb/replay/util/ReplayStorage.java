package me.justindevb.replay.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReplayStorage {
    private final File replayFolder;

    public ReplayStorage(JavaPlugin plugin) {
        this.replayFolder = new File(plugin.getDataFolder(), "replays");
        if (!replayFolder.exists())
            replayFolder.mkdirs();
    }

    public List<String> listReplays() {
        String[] files = replayFolder.list((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return Collections.emptyList();
        return Arrays.stream(files)
                .map(name -> name.substring(0, name.length() - 5))
                .collect(Collectors.toList());
    }

    public boolean deleteReplay(String name) {
        File f = new File(replayFolder, name + ".json");
        return f.exists() && f.delete();
    }

    public File getReplayFile(String name) {
        return new File(replayFolder, name + ".json");
    }
}
