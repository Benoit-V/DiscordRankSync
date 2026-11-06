package fr.bebedlastreat.discord.common.utils;

import fr.bebedlastreat.discord.common.DiscordCommon;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Wraps a repeating task so that a run is skipped while the previous one is still running.
 * <p>
 * Bukkit, BungeeCord and Velocity start repeating async tasks without waiting for the previous run, on unbounded
 * thread pools. A slow run (blocking Discord or database calls, Discord rate limit) would otherwise pile up
 * threads until the JVM can't create new ones and crashes.
 */
public final class NonOverlappingTask implements Runnable {

    private final String name;
    private final Runnable delegate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NonOverlappingTask(String name, Runnable delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            DiscordCommon.getLogger().log(Level.WARNING, "Skipping " + name + ": the previous run is still in progress");
            return;
        }
        try {
            delegate.run();
        } catch (Throwable t) {
            DiscordCommon.getLogger().log(Level.SEVERE, "Error in " + name + ": " + t.getMessage());
        } finally {
            running.set(false);
        }
    }
}
