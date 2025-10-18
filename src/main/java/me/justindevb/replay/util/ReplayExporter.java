package me.justindevb.replay.util;

// ReplayExporter.java
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.Base64;

public class ReplayExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Simple model classes used for export
    public static class ExportReplay {
        public int schemaVersion = 1;
        public int tickRate = 20;
        public String world;
        public long startTime;
        public int durationTicks;
        public List<ExportEntity> entities = new ArrayList<>();
        public List<ExportChunk> chunks = new ArrayList<>();
        public List<Frame> frames = new ArrayList<>();
        // optional raw packet timeline if you want to retain fidelity
        public List<Map<String,Object>> rawPackets = null;
    }
    public static class ExportEntity {
        public int exportId;
        public int serverEntityId;
        public String uuid;
        public String type;
        public String name;
        public List<Map<String,String>> skin; // list of {name,value,signature}
    }
    public static class ExportChunk {
        public int cx, cz;
        public String data; // base64(gzip(nbt bytes))
    }
    public static class Frame {
        public int tick;
        public List<Map<String,Object>> events = new ArrayList<>();
    }

    /**
     * Convert a recording file (JSON timeline) into a web-friendly export.
     * If gzipOutput == true, write file as gzipped JSON.
     */
    public static void export(File recordingJson, File out, boolean gzipOutput) throws IOException {
        String raw = new String(Files.readAllBytes(recordingJson.toPath()));
        List<Map<String, Object>> timeline = GSON.fromJson(raw, new TypeToken<List<Map<String, Object>>>(){}.getType());

        ExportReplay export = new ExportReplay();

        // Example metadata heuristics (you can override)
        export.world = "world";
        export.startTime = System.currentTimeMillis() / 1000L;
        export.durationTicks = estimateDurationTicks(timeline);

        // 1) Build entity templates (first-seen event provides metadata)
        Map<Integer, ExportEntity> entityMap = new LinkedHashMap<>();
        int nextExportId = 1;
        for (Map<String,Object> ev : timeline) {
            Number n = (Number) ev.get("entityId");
            if (n == null) continue;
            int serverId = n.intValue();
            if (!entityMap.containsKey(serverId)) {
                ExportEntity e = new ExportEntity();
                e.exportId = nextExportId++;
                e.serverEntityId = serverId;
                Object uu = ev.get("uuid");
                e.uuid = uu != null ? uu.toString() : null;
                Object nm = ev.get("name");
                e.name = nm != null ? nm.toString() : ("entity-" + serverId);
                Object t = ev.get("etype");
                e.type = t != null ? t.toString() : (ev.get("type") != null ? ev.get("type").toString() : "UNKNOWN");
                // skin: if you recorded texture properties as 'textures' in frames, capture them
                Object textures = ev.get("textures"); // adapt to your recording format
                if (textures instanceof List) {
                    // assume each item is Map with name,value,signature
                    List<Map<String,String>> skinProps = new ArrayList<>();
                    for (Object o : (List)textures) {
                        if (o instanceof Map) {
                            Map m = (Map)o;
                            Map<String,String> prop = new HashMap<>();
                            prop.put("name", String.valueOf(m.getOrDefault("name","textures")));
                            prop.put("value", String.valueOf(m.get("value")));
                            if (m.get("signature") != null) prop.put("signature", String.valueOf(m.get("signature")));
                            skinProps.add(prop);
                        }
                    }
                    e.skin = skinProps;
                }
                entityMap.put(serverId, e);
            }
        }
        export.entities.addAll(entityMap.values());

        // 2) Convert timeline to per-tick frames with normalized events
        Map<Integer, Frame> frames = new LinkedHashMap<>();
        for (Map<String,Object> ev : timeline) {
            Number tickN = (Number) ev.get("tick");
            if (tickN == null) continue;
            int tick = tickN.intValue();
            Frame f = frames.computeIfAbsent(tick, k -> { Frame ff = new Frame(); ff.tick = k; return ff; });
            Map<String,Object> webEvent = convertToWebEvent(ev, entityMap);
            if (webEvent != null) f.events.add(webEvent);
        }
        export.frames.addAll(frames.values());

        // 3) OPTIONAL: include chunks if your recording has them
        // If you store chunk NBT bytes as raw byte[] in a sidecar map, encode them:
        // Example: Map<String, byte[]> recordedChunks = ...  (key "cx,cz")
        // for (Map.Entry<String,byte[]> entry: recordedChunks.entrySet()) { ... base64(GZIP(bytes)) ... }

        // 4) Optionally keep raw packets (if you want high-fidelity re-injection later)
        export.rawPackets = timeline; // WARNING: can be huge

        // 5) write JSON (optionally gzipped)
        if (gzipOutput) {
            try (FileOutputStream fos = new FileOutputStream(out);
                 GZIPOutputStream gos = new GZIPOutputStream(fos);
                 OutputStreamWriter osw = new OutputStreamWriter(gos);
                 BufferedWriter bw = new BufferedWriter(osw)) {
                GSON.toJson(export, bw);
            }
        } else {
            try (FileWriter fw = new FileWriter(out)) {
                GSON.toJson(export, fw);
            }
        }
    }

    private static int estimateDurationTicks(List<Map<String,Object>> timeline) {
        int max = 0;
        for (Map<String,Object> ev : timeline) {
            Number t = (Number)ev.get("tick");
            if (t != null && t.intValue() > max) max = t.intValue();
        }
        return max + 1;
    }

    /**
     * Convert a recorded raw event to a normalized, web-friendly event map.
     * This is the key mapping: translate packet-like data to simple semantic events.
     */
    private static Map<String,Object> convertToWebEvent(Map<String,Object> raw, Map<Integer, ExportEntity> entityMap) {
        Map<String,Object> out = new LinkedHashMap<>();
        String rawType = (String) raw.get("type");
        Number entityIdN = (Number) raw.get("entityId");
        if (entityIdN != null) {
            int sid = entityIdN.intValue();
            ExportEntity e = entityMap.get(sid);
            out.put("entity", e != null ? e.exportId : sid);
        }

        switch (rawType) {
            case "player_move":
            case "entity_move":
                out.put("type","move");
                out.put("x", raw.get("x"));
                out.put("y", raw.get("y"));
                out.put("z", raw.get("z"));
                out.put("yaw", raw.get("yaw"));
                out.put("pitch", raw.get("pitch"));
                // optional: velocity and onGround if present
                if (raw.get("vx") != null) out.put("vx", raw.get("vx"));
                if (raw.get("vy") != null) out.put("vy", raw.get("vy"));
                if (raw.get("vz") != null) out.put("vz", raw.get("vz"));
                if (raw.get("onGround") != null) out.put("onGround", raw.get("onGround"));
                return out;

            case "swing":
            case "animation":
                out.put("type","swing");
                return out;

            case "use_entity":
            case "attack":
                out.put("type","attack");
                out.put("targetEntity", raw.get("targetId"));
                out.put("hand", raw.getOrDefault("hand","main"));
                return out;

            case "block_place":
                out.put("type","block_place");
                out.put("x", raw.get("x"));
                out.put("y", raw.get("y"));
                out.put("z", raw.get("z"));
                out.put("block", raw.get("block"));
                return out;

            case "block_break":
                out.put("type","block_break");
                out.put("x", raw.get("x"));
                out.put("y", raw.get("y"));
                out.put("z", raw.get("z"));
                out.put("block", raw.get("block"));
                return out;

            case "chat":
                out.put("type","chat");
                out.put("message", raw.get("message"));
                return out;

            // add more cases here for other packet types you record

            default:
                // If unknown, you may want to keep raw as-is under a 'raw' event
                Map<String,Object> rawcopy = new LinkedHashMap<>(raw);
                out.put("type","raw");
                out.put("payload", rawcopy);
                return out;
        }
    }
}

