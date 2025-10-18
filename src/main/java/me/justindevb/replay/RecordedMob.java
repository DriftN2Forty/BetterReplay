package me.justindevb.replay;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.justindevb.replay.util.SpawnFakeMob;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RecordedMob extends RecordedEntity {

    public RecordedMob(UUID uuid, EntityType type, Player viewer) {
        super(uuid, type, viewer);
    }

    @Override
    public void spawn(Location loc) {
        // Use a spawn packet for generic mobs
        new SpawnFakeMob(type, loc, viewer);
    }

    @Override
    public void moveTo(Location loc) {
        //SpawnFakeMob.teleport(fakeEntityId, loc, viewer);
    }
}

