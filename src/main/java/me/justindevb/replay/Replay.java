package me.justindevb.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.tcoded.folialib.FoliaLib;
import org.bstats.bukkit.Metrics;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.justindevb.replay.api.ReplayAPI;
import me.justindevb.replay.listeners.PacketEventsListener;
import me.justindevb.replay.util.ReplayCache;
import me.justindevb.replay.util.UpdateChecker;
import me.justindevb.replay.storage.FileReplayStorage;
import me.justindevb.replay.storage.MySQLConnectionManager;
import me.justindevb.replay.storage.MySQLReplayStorage;
import me.justindevb.replay.storage.ReplayStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.logging.Level;

public class Replay extends JavaPlugin {
    private static Replay instance;
    private RecorderManager recorderManager;
    private ReplayStorage storage = null;
    private MySQLConnectionManager connectionManager;
    private ReplayCache replayCache;
    private ReplayManagerImpl manager;
    private FoliaLib foliaLib;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(this), PacketListenerPriority.LOWEST);

    }

    @Override
    public void onEnable() {
        instance = this;
        PacketEvents.getAPI().init();
        foliaLib = new FoliaLib(this);

        recorderManager = new RecorderManager(this);
        manager = new ReplayManagerImpl(this, recorderManager);
        ReplayCommand replayCommand = new ReplayCommand(manager);
        initConfig();

        PluginCommand cmd = getCommand("replay");
        if (cmd != null) {
            cmd.setExecutor(replayCommand);
            cmd.setTabCompleter(replayCommand);
        }

        //Initialize API
        ReplayAPI.init(manager);

        initStorage();

        initBstats();


        checkForUpdate();
    }

    @Override
    public void onDisable() {
        recorderManager.shutdown();

        for (ReplaySession session : ReplayRegistry.getActiveSessions()) {
            if (session != null)
                session.stop();
        }

        PacketEvents.getAPI().terminate();
        ReplayAPI.shutdown();

        if (connectionManager != null)
            connectionManager.shutdown();



        instance = null;
    }

    public static Replay getInstance() {
        return instance;
    }

    public RecorderManager getRecorderManager() {
        return recorderManager;
    }

    public ReplayStorage getReplayStorage() {
        return storage;
    }

    private void initConfig() {
        initGeneralConfigSettings();

        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void initGeneralConfigSettings() {
        FileConfiguration config = getConfig();
        for (ConfigSetting setting : ConfigSetting.values()) {
            setting.applyDefault(config);
        }
    }

    private void checkForUpdate() {
        if (!ConfigSetting.CHECK_UPDATE.getBoolean(getConfig()))
            return;

        String currentVersion = getPluginMeta().getVersion();
        new UpdateChecker(this, "betterreplay").checkForUpdate(currentVersion, result -> {
            if (result.updateAvailable()) {
                String suffix = "release".equals(result.versionType()) ? "" : " (" + result.versionType() + ")";
                getLogger().log(Level.INFO, "Update available: v" + result.latestVersion() + suffix
                        + " — https://modrinth.com/plugin/betterreplay");
            } else {
                getLogger().log(Level.INFO, "You are up to date!");
            }
        });
    }

    private void initStorage() {
        FileConfiguration config = getConfig();
        String storageType = ConfigSetting.STORAGE_TYPE.getString(config).toLowerCase(Locale.ROOT);
        if (storageType.contentEquals("mysql")) {
            String host = ConfigSetting.MYSQL_HOST.getString(config);
            int port = ConfigSetting.MYSQL_PORT.getInt(config);
            String database = ConfigSetting.MYSQL_DATABASE.getString(config);
            String user = ConfigSetting.MYSQL_USER.getString(config);
            String password = ConfigSetting.MYSQL_PASSWORD.getString(config);

            connectionManager = new MySQLConnectionManager(host, port, database, user, password);

            storage = new MySQLReplayStorage(connectionManager.getDataSource(), this);
        } else if (storageType.contentEquals("file")) {
            storage = new FileReplayStorage(this);
        } else {
            getLogger().log(Level.SEVERE, "Invalid storage selected: " + storageType);
            getLogger().log(Level.SEVERE, "Valid types: file, mysql");
            getLogger().log(Level.SEVERE, "Defaulting to file");
            storage = new FileReplayStorage(this);
        }

        replayCache = new ReplayCache();
        getReplayStorage().listReplays().thenAccept(replays -> replayCache.setReplays(replays));
    }

    public ReplayCache getReplayCache() {
        return replayCache;
    }

    public ReplayManagerImpl getReplayManagerImpl() {
        return manager;
    }

    public void initBstats() {
        int pluginId = 29341;
        new Metrics(this, pluginId);
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public enum ConfigSetting {
        CHECK_UPDATE("General.Check-Update", true),
        COMPRESS_REPLAYS("General.Compress-Replays", true),
        STORAGE_TYPE("General.Storage-Type", "file"),
        MYSQL_HOST("General.MySQL.host", "host"),
        MYSQL_PORT("General.MySQL.port", 3306),
        MYSQL_DATABASE("General.MySQL.database", "database"),
        MYSQL_USER("General.MySQL.user", "username"),
        MYSQL_PASSWORD("General.MySQL.password", "password"),
        LIST_PAGE_SIZE("list-page-size", 10);

        private final String key;
        private final Object defaultValue;

        ConfigSetting(String key, Object defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public void applyDefault(FileConfiguration config) {
            config.addDefault(this.key, this.defaultValue);
        }

        public String getString(FileConfiguration config) {
            return config.getString(this.key, (String) this.defaultValue);
        }

        public boolean getBoolean(FileConfiguration config) {
            return config.getBoolean(this.key, (boolean) this.defaultValue);
        }

        public int getInt(FileConfiguration config) {
            return config.getInt(this.key, (int) this.defaultValue);
        }

        public String getKey() {
            return key;
        }
    }
}