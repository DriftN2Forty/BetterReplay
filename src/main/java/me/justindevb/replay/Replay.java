package me.justindevb.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.justindevb.replay.listeners.PacketEventsListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Replay extends JavaPlugin {
    private static Replay instance;
    private RecorderManager recorderManager;

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

        recorderManager = new RecorderManager(this);
        ReplayCommand replayCommand = new ReplayCommand(recorderManager);

        PluginCommand cmd = getCommand("replay");
        if (cmd != null) {
            cmd.setExecutor(replayCommand);
            cmd.setTabCompleter(replayCommand);
        }
    }

    @Override
    public void onDisable() {
        recorderManager.shutdown();
        PacketEvents.getAPI().terminate();


        instance = null;
    }

    public static Replay getInstance() {
        return instance;
    }

    public RecorderManager getRecorderManager() {
        return recorderManager;
    }
}