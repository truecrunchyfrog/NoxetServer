package org.noxet.noxetserver.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.noxet.noxetserver.NoxetServer;

public class Promise {
    private final Runnable callback;
    private boolean callbackUsed = false;
    private final BukkitTask timeoutTask;

    /**
     * Run a callback when a player has reported a process as finished.
     * @param callback Something to do when the process is reported
     * @param ticksToReport The time to wait for the process to finish (after passed, the callback will be run anyway)
     */
    public Promise(Runnable callback, int ticksToReport) {
        this.callback = callback;
        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                report();
            }
        }.runTaskLater(NoxetServer.getPlugin(), ticksToReport);
    }

    /**
     * Call when the heavy process has finished.
     */
    public void report() {
        if(!callbackUsed) {
            callbackUsed = true;

            timeoutTask.cancel();

            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.run();
                }
            }.runTask(NoxetServer.getPlugin());
        }
    }

    /**
     * Check if the promise has been reported.
     * @return true if the promise has been reported, otherwise false
     */
    public boolean isReported() {
        return callbackUsed;
    }
}
