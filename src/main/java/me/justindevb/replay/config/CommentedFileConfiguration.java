package me.justindevb.replay.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal comment-preserving YAML wrapper for Bukkit configs.
 * It stores comments as temporary pseudo-keys during parse and restores them on save.
 */
public class CommentedFileConfiguration {

    private static final String COMMENT_MARKER = "_COMMENT_";

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private int commentIndex;

    public CommentedFileConfiguration(Plugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        this.yaml = new YamlConfiguration();
        this.commentIndex = 0;
    }

    public void load() {
        ensureParentDirectory();
        if (!file.exists()) {
            this.yaml = new YamlConfiguration();
            this.commentIndex = 0;
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file: " + file.getAbsolutePath(), e);
        }

        this.commentIndex = countCommentLines(lines);
        String transformed = transformCommentsToKeys(lines);
        try (Reader reader = new StringReader(transformed)) {
            this.yaml = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config content", e);
        }
    }

    public boolean setIfNotExists(ReplayConfigSetting setting) {
        if (yaml.get(setting.getKey()) != null) {
            return false;
        }

        String prefix = "";
        int split = setting.getKey().lastIndexOf('.');
        if (split != -1) {
            prefix = setting.getKey().substring(0, split + 1);
        }

        for (String comment : setting.getComments()) {
            yaml.set(prefix + plugin.getName() + COMMENT_MARKER + commentIndex++, " " + comment);
        }
        yaml.set(setting.getKey(), setting.getDefaultValue());
        return true;
    }

    public void addHeaderComments(String... comments) {
        for (String comment : comments) {
            yaml.set(plugin.getName() + COMMENT_MARKER + commentIndex++, " " + comment);
        }
    }

    public void save() {
        ensureParentDirectory();
        String rawYaml = yaml.saveToString();
        String restored = restoreComments(rawYaml);

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(restored);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file: " + file.getAbsolutePath(), e);
        }
    }

    private void ensureParentDirectory() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private int countCommentLines(List<String> lines) {
        int count = 0;
        for (String line : lines) {
            if (line.trim().startsWith("#")) {
                count++;
            }
        }
        return count;
    }

    private String transformCommentsToKeys(List<String> lines) {
        StringBuilder out = new StringBuilder();
        int idx = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("#")) {
                out.append(line).append('\n');
                continue;
            }

            int indent = line.indexOf(trimmed);
            String indentStr = indent > 0 ? line.substring(0, indent) : "";
            String escaped = line.replace("'", "''");
            String pseudo = escaped.replaceFirst("#", plugin.getName() + COMMENT_MARKER + idx++ + ": '") + "'";
            out.append(indentStr).append(pseudo.trim()).append('\n');
        }

        return out.toString();
    }

    private String restoreComments(String yamlContent) {
        String[] lines = yamlContent.split("\\r?\\n", -1);
        List<String> restored = new ArrayList<>(lines.length);
        String markerPrefix = plugin.getName() + COMMENT_MARKER;

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith(markerPrefix)) {
                restored.add(line);
                continue;
            }

            int indent = line.indexOf(trimmed);
            String indentStr = indent > 0 ? line.substring(0, indent) : "";
            int colon = line.indexOf(':');
            if (colon < 0 || colon + 2 >= line.length()) {
                continue;
            }

            String value = line.substring(colon + 1).trim();
            if (value.startsWith("'")) {
                value = value.substring(1);
            }
            if (value.endsWith("'")) {
                value = value.substring(0, value.length() - 1);
            }
            value = value.replace("''", "'");

            if (value.startsWith(" ")) {
                restored.add(indentStr + "#" + value);
            } else {
                restored.add(indentStr + "# " + value);
            }
        }

        return String.join(System.lineSeparator(), restored);
    }
}
