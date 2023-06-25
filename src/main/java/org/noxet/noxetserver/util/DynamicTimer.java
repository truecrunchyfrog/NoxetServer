package org.noxet.noxetserver.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.noxet.noxetserver.NoxetServer;

public abstract class DynamicTimer extends BukkitRunnable {
    private BukkitTask timer;

    /**
     * Used to test if a timer is necessary. This is to save CPU by disabling the timer when unnecessary.
     * Check whether, for example, a variable is empty or such.
     * This method will be invoked with {@code touchTimer()} to check whether a timer should be created/stopped.
     * @return true if the timer should be running, otherwise false
     */
    public abstract boolean isTimerNecessary();

    /**
     * The delay - in ticks - to invoke {@code run()}.
     * @return The delay in ticks (lower = faster)
     */
    public abstract int getTickFrequency();

    private void assignTimer() {
        stopTimer();

        timer = this.runTaskTimer(NoxetServer.getPlugin(), 0, getTickFrequency());
    }

    private void stopTimer() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Should be called after any change to expression used in {@code isTimerNecessary()}.
     */
    public void touchTimer() {
        if(isTimerNecessary() && timer == null)
            assignTimer();
        else if(!isTimerNecessary() && timer != null)
            stopTimer();
    }
}
