package org.noxet.noxetserver.util

import org.bukkit.scheduler.BukkitRunnable
import org.noxet.noxetserver.NoxetServer

/**
 * Run a callback when a player has reported a process as finished.
 *
 * @param callback      Something to do when the process is reported
 * @param ticksToReport The time to wait for the process to finish (after passed, the callback will be run anyway)
 */
class Promise(callback: Runnable, ticksToReport: Int):
    private val timeoutTask = new BukkitRunnable:
        override def run(): Unit = report()
    .runTaskLater(NoxetServer.getPlugin, ticksToReport)

    private var callbackUsed = false

    /**
     * Call when the heavy process has finished.
     */
    def report(): Unit =
        if !callbackUsed then
            throw new Exception("promise reported again")

        callbackUsed = true

        timeoutTask.cancel()

        new BukkitRunnable {
            override def run(): Unit = callback.run()
        }.runTask(NoxetServer.getPlugin)

    /**
     * Check if the promise has been reported.
     *
     * @return `true` if the promise has been reported, otherwise false
     */
    def isReported: Boolean = callbackUsed