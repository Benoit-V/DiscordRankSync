package fr.bebedlastreat.discord.common.redis;

import fr.bebedlastreat.discord.common.DiscordCommon;
import lombok.Data;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Data
public class RedisHandler {

    /**
     * Maximum age of a forwarded command. Also bounds the replay-protection cache.
     */
    private static final long COMMAND_MAX_AGE = 60_000L;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String host;
    private final int port;
    private final String password;
    /**
     * Shared secret used to sign forwarded console commands. Empty = command forwarding disabled.
     */
    private final String secret;
    private final Map<String, Long> seenSignatures = new ConcurrentHashMap<>();

    private JedisPool jedisPool;

    public RedisHandler(String host, int port, String password, String secret) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.secret = secret == null ? "" : secret;
    }

    public void init() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(40);
        jedisPoolConfig.setMaxIdle(20);
        jedisPoolConfig.setMinIdle(1);
        if (password == null || password.isEmpty()) {
            jedisPool = new JedisPool(jedisPoolConfig, host, port, 5000);
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, host, port, 5000, password);
        }
    }

    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public Jedis getResource() {
        return jedisPool.getResource();
    }

    public void send(String channel, String message) {
        try (Jedis jedis = getResource()) {
            jedis.publish(channel, message);
        }
    }

    public void subscribe(String channel, JedisPubSub subscriber) {
        try (Jedis jedis = getResource()) {
            jedis.subscribe(subscriber, channel);
        }
    }

    public boolean isCommandForwardingEnabled() {
        return !secret.isEmpty();
    }

    /**
     * Publishes a console command for the backend servers, signed with the shared secret.
     *
     * @return false if command forwarding is disabled (no secret configured)
     */
    public boolean sendCommand(String command) {
        if (!isCommandForwardingEnabled()) {
            DiscordCommon.getLogger().log(Level.WARNING, "redis.secret is empty, can't forward command: " + command);
            return false;
        }
        String payload = System.currentTimeMillis() + ":" + command;
        send(DiscordCommon.COMMAND_CHANNEL, sign(payload) + ":" + payload);
        return true;
    }

    /**
     * Checks the signature and freshness of a message received on the command channel.
     *
     * @return the command to execute, or null if the message must be rejected
     */
    public String verifyCommand(String message) {
        if (!isCommandForwardingEnabled()) {
            DiscordCommon.getLogger().log(Level.WARNING, "Received a forwarded command but redis.secret is empty, ignoring it");
            return null;
        }
        int sigEnd = message.indexOf(':');
        if (sigEnd <= 0) return reject("malformed message");
        String signature = message.substring(0, sigEnd);
        String payload = message.substring(sigEnd + 1);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), sign(payload).getBytes(StandardCharsets.UTF_8))) {
            return reject("invalid signature");
        }
        int tsEnd = payload.indexOf(':');
        if (tsEnd <= 0) return reject("malformed message");
        long timestamp;
        try {
            timestamp = Long.parseLong(payload.substring(0, tsEnd));
        } catch (NumberFormatException ex) {
            return reject("malformed timestamp");
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > COMMAND_MAX_AGE) return reject("expired message");

        seenSignatures.values().removeIf(time -> now - time > COMMAND_MAX_AGE * 2);
        if (seenSignatures.putIfAbsent(signature, now) != null) return reject("replayed message");
        return payload.substring(tsEnd + 1);
    }

    private String reject(String reason) {
        DiscordCommon.getLogger().log(Level.WARNING, "Rejected a forwarded command from redis: " + reason);
        return null;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Can't sign redis message", e);
        }
    }
}
