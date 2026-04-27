package me.justindevb.replay.storage.binary;

import me.justindevb.replay.recording.TimelineEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32C;

/**
 * Reads append-log temp files written by {@link BinaryReplayAppendLogWriter}.
 */
public final class BinaryReplayAppendLogReader {

    public List<TimelineEvent> readTimeline(Path path) throws IOException {
        List<String> stringTable = new ArrayList<>();
        List<TimelineEvent> timeline = new ArrayList<>();
        for (DecodedRecord record : readRecords(path)) {
            if (record.type() == BinaryRecordType.DEFINE_STRING) {
                BinaryReplayAppendLogCodec.DefinedString definedString = BinaryReplayAppendLogCodec.decodeDefineString(record.payload());
                if (definedString.index() != stringTable.size()) {
                    throw new IOException("Unexpected string-table index " + definedString.index() + " while reading append-log");
                }
                stringTable.add(definedString.value());
                continue;
            }
            timeline.add(BinaryReplayAppendLogCodec.decodeEvent(record.type(), record.payload(), stringTable));
        }
        return timeline;
    }

    public List<DecodedRecord> readRecords(Path path) throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }

        byte[] bytes = Files.readAllBytes(path);
        BinaryReplayAppendLogCodec.Cursor cursor = new BinaryReplayAppendLogCodec.Cursor(bytes);
        List<DecodedRecord> records = new ArrayList<>();
        while (cursor.remainingBytes().length > 0) {
            int recordLength = cursor.readVarInt();
            byte[] recordContent = cursor.readBytes(recordLength);
            int storedChecksum = cursor.readInt();
            int computedChecksum = calculateCrc32c(recordContent);
            if (storedChecksum != computedChecksum) {
                throw new IOException("Append-log CRC32C mismatch");
            }

            BinaryReplayAppendLogCodec.Cursor recordCursor = new BinaryReplayAppendLogCodec.Cursor(recordContent);
            int recordTypeTag = recordCursor.readVarInt();
            BinaryRecordType recordType = BinaryRecordType.fromTag(recordTypeTag)
                    .orElseThrow(() -> new IOException("Unknown append-log record tag: " + recordTypeTag));
            records.add(new DecodedRecord(recordType, recordCursor.remainingBytes(), storedChecksum, recordContent));
        }
        return records;
    }

    private static int calculateCrc32c(byte[] recordContent) {
        CRC32C crc32c = new CRC32C();
        crc32c.update(recordContent, 0, recordContent.length);
        return (int) crc32c.getValue();
    }

    public record DecodedRecord(BinaryRecordType type, byte[] payload, int storedChecksum, byte[] checksummedBytes) {
    }
}