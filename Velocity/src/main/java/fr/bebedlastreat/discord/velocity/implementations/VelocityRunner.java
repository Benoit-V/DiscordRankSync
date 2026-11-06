package fr.bebedlastreat.discord.velocity.implementations;

import fr.bebedlastreat.discord.common.interfaces.ICommonRunner;
import fr.bebedlastreat.discord.velocity.DiscordSyncVelocity;

import java.util.concurrent.TimeUnit;

public class VelocityRunner implements ICommonRunner {

    // Velocity has no main thread: "sync" code can run directly on the calling thread
    @Override
    public void run(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void runAsync(Runnable runnable) {
        DiscordSyncVelocity.getServer().getScheduler().buildTask(DiscordSyncVelocity.getInstance(), runnable).schedule();
    }

    @Override
    public void runLater(Runnable runnable, int ticks) {
        DiscordSyncVelocity.getServer().getScheduler().buildTask(DiscordSyncVelocity.getInstance(), runnable).delay(ticks * 50L, TimeUnit.MILLISECONDS).schedule();
    }

    @Override
    public void runTask(Runnable runnable, int delay, int ticks) {
        DiscordSyncVelocity.getServer().getScheduler().buildTask(DiscordSyncVelocity.getInstance(), runnable).delay(delay * 50L, TimeUnit.MILLISECONDS).repeat(ticks * 50L, TimeUnit.MILLISECONDS).schedule();
    }
}
