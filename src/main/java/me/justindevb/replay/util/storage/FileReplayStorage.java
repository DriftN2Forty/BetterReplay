package me.justindevb.replay.util.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.justindevb.replay.Replay;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FileReplayStorage implements ReplayStorage {

    private final File replayFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FileReplayStorage(Replay replay) {
        this.replayFolder = new File(replay.getDataFolder(), "replays");
        if (!replayFolder.exists())
            replayFolder.mkdirs();
    }

    @Override
    public CompletableFuture<Void> saveReplay(String name, List<?> timeline) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(replayFolder, name + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(timeline, writer);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save replay " + name, e);
            }
        });
    }

    @Override
    public CompletableFuture<List<?>> loadReplay(String name) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(replayFolder, name + ".json");
            if (!file.exists()) return null;

            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, List.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load replay " + name, e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteReplay(String name) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(replayFolder, name + ".json");
            return file.exists() && file.delete();
        });
    }

    @Override
    public CompletableFuture<List<String>> listReplays() {
        return CompletableFuture.supplyAsync(() -> {
            String[] files = replayFolder.list((dir, n) -> n.endsWith(".json"));
            List<String> names = new ArrayList<>();
            if (files != null) {
                for (String f : files) names.add(f.substring(0, f.length() - 5));
            }
            return names;
        });
    }

    @Override
    public CompletableFuture<Boolean> replayExists(String name) {
        return CompletableFuture.supplyAsync(() -> new File(replayFolder, name + ".json").exists());
    }

    @Override
    public CompletableFuture<File> getReplayFile(String name) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(replayFolder, name + ".json");
            return file.exists() && file.isFile() ? file : null;
        });
    }
}

/*public class FileReplayStorage {
    private final File replayFolder;

    public FileReplayStorage(JavaPlugin plugin) {
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
*/