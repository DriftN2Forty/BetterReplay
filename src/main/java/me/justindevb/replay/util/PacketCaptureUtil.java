package me.justindevb.replay.util;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PacketCaptureUtil {
    public static Map<String, Object> captureIncoming(Player player, PacketReceiveEvent event, int tick) {
        Map<String, Object> frame = new HashMap<>();
        frame.put("tick", tick);
        frame.put("entityId", player.getEntityId());

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            frame.put("type", "attack");
            frame.put("targetId", interact.getEntityId());
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            frame.put("type", "block_dig");
            frame.put("blockPos", dig.getBlockPosition().toString());
        } else {
            return null; // not recording this type yet
        }

        return frame;
    }

    public static Map<String, Object> captureOutgoing(Player player, PacketSendEvent event, int tick) {
        Map<String, Object> frame = new HashMap<>();
        frame.put("tick", tick);
        frame.put("entityId", player.getEntityId());

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            frame.put("type", "pose_change");
            // extract metadata
        } else {
            return null; // ignore for now
        }

        return frame;
    }
}

