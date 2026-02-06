package me.justindevb.replay.util.storage;

import com.google.gson.Gson;
import me.justindevb.replay.Replay;
import org.bukkit.Bukkit;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MySQLReplayStorage implements ReplayStorage {

    private final DataSource dataSource;
    private final Gson gson = new Gson();
    private final Replay replay;

    public MySQLReplayStorage(DataSource dataSource, Replay replay) {
        this.dataSource = dataSource;
        this.replay = replay;
        init();
    }

    private void init() {
        //Bukkit.getScheduler().runTaskAsynchronously(replay, () -> {
        replay.getFoliaLib().getScheduler().runAsync(task -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS replays (
                        name VARCHAR(64) PRIMARY KEY,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        data MEDIUMBLOB NOT NULL
                    )
                """);

            } catch (SQLException e) {
                replay.getLogger().severe("Failed to init replay table");
                e.printStackTrace();
            }
        });
    }


    @Override
    public CompletableFuture<Void> saveReplay(String name, List<?> timeline) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO replays (name, data)
                 VALUES (?, ?)
                 ON DUPLICATE KEY UPDATE data = VALUES(data)
             """)) {

                byte[] data = compress(gson.toJson(timeline));

                ps.setString(1, name);
                ps.setBytes(2, data);
                ps.executeUpdate();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Override
    public CompletableFuture<List<?>> loadReplay(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT data FROM replays WHERE name=?"
                 )) {

                ps.setString(1, name);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return gson.fromJson(decompress(rs.getBytes("data")), List.class);
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to load replay: " + name, e);
            }
        });
    }


    @Override
    public CompletableFuture<Boolean> replayExists(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT 1 FROM replays WHERE name=? LIMIT 1"
                 )) {

                ps.setString(1, name);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }


    @Override
    public CompletableFuture<Boolean> deleteReplay(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM replays WHERE name=?"
                 )) {

                ps.setString(1, name);
                int affected = ps.executeUpdate();
                return affected > 0; // true if a row was deleted

            } catch (Exception e) {
                throw new RuntimeException("Failed to delete replay: " + name, e);
            }
        });
    }


    @Override
    public CompletableFuture<List<String>> listReplays() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> names = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT name FROM replays ORDER BY created_at DESC"
                 );
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
                return names;

            } catch (Exception e) {
                throw new RuntimeException("Failed to list replays", e);
            }
        });
    }

    @Override
    public CompletableFuture<File> getReplayFile(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT data FROM replays WHERE name=?")) {

                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        return null;

                    byte[] compressedData = rs.getBytes("data");
                    List<?> timeline = gson.fromJson(decompress(compressedData), List.class);

                    File tempFile = File.createTempFile("replay_" + name + "_", ".json");
                    tempFile.deleteOnExit();
                    try (FileWriter writer = new FileWriter(tempFile)) {
                        gson.toJson(timeline, writer);
                    }
                    return tempFile;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }


    private byte[] compress(String s) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(s.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private String decompress(byte[] bytes) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
