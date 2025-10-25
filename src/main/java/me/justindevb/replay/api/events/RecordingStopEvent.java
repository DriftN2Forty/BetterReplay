package me.justindevb.replay.api.events;

import me.justindevb.replay.RecordingSession;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RecordingStopEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final RecordingSession session;

    public RecordingStopEvent(RecordingSession session) {
        this.session = session;
    }

    public RecordingSession getSession() {
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
