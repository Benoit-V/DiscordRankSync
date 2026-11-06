package fr.bebedlastreat.discord.common.redis;

import lombok.Data;

import java.io.*;
import java.util.Base64;
import java.util.UUID;

/**
 * Encodes player Discord data sent over redis. The binary payload is Base64 encoded: converting raw
 * {@code writeUTF} bytes to a UTF-8 String corrupts names containing emojis or longer than 127 bytes.
 */
public final class PlayerDataCodec {

    private PlayerDataCodec() {
    }

    public static String encode(UUID uuid, boolean linked, String discordName, boolean boosting) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(uuid.toString());
            out.writeBoolean(linked);
            out.writeUTF(discordName);
            out.writeBoolean(boosting);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /**
     * @throws IllegalArgumentException if the message is not a valid payload
     */
    public static PlayerData decode(String message) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(message)))) {
            UUID uuid = UUID.fromString(in.readUTF());
            boolean linked = in.readBoolean();
            String discordName = in.readUTF();
            boolean boosting = in.readBoolean();
            return new PlayerData(uuid, linked, discordName, boosting);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid player data payload", e);
        }
    }

    @Data
    public static final class PlayerData {
        private final UUID uuid;
        private final boolean linked;
        private final String discordName;
        private final boolean boosting;
    }
}
