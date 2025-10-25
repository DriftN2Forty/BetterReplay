package me.justindevb.replay.api.events;

import me.justindevb.replay.ReplaySession;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ReplayStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player viewer;
    private final File replayFile;
    private final ReplaySession session;

    public ReplayStartEvent(@NotNull Player viewer, @NotNull File replayFile, @NotNull ReplaySession session) {
        this.viewer = viewer;
        this.replayFile = replayFile;
        this.session = session;
    }

    /**
     *
     * @return Who is watching the replay
     */
    public @NotNull Player getViewer() {
        return viewer;
    }

    public @NotNull File getReplayFile() {
        return replayFile;
    }

    public @NotNull ReplaySession getSession() {
        return session;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}

