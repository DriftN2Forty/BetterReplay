package me.justindevb.replay.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import me.justindevb.replay.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketEventsListener implements PacketListener {
    private final Replay replay;

    public PacketEventsListener(Replay replay) {
        this.replay = replay;
    }


    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        RecorderManager manager = Replay.getInstance().getRecorderManager();

        if (!event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY))
            return;

        Player viewer = (Player) event.getPlayer();
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        int entityId = wrapper.getEntityId();
        RecordedEntity clicked = ReplayRegistry.getEntityById(entityId);

        if (clicked == null) {
            return;
        }

        if (clicked instanceof RecordedPlayer rp) {
            Bukkit.getScheduler().runTask(Replay.getInstance(), () -> {
                rp.openInventoryForViewer(viewer);
            });
            event.setCancelled(true);
        }

    }


}
