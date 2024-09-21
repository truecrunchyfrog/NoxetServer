package org.noxet.noxetserver.util

import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.noxet.noxetserver.NoxetServer

trait DynamicTimer:
  private var timer: Option[BukkitTask] = None

  /**
   * Used to test if a timer is necessary. This is to save CPU by disabling the timer when unnecessary.
   * Check whether, for example, a variable is empty or such.
   * This method will be invoked with [[touchTimer]] to check whether a timer should be created/stopped.
   *
   * @return `true` if the timer needs to be running, otherwise `false`
   */
  def isTimerNecessary: Boolean

  /**
   * The delay - in ticks - to invoke [[Runnable]].
   *
   * @return The delay in ticks (lower = faster)
   */
  def getTickFrequency: Int

  private def assignTimer(): Unit =
    stopTimer()

    timer = Some(QuickRunnable(timerCall())
      .runTaskTimer(NoxetServer.getPlugin, 0, getTickFrequency))


  private def stopTimer(): Unit =
    timer.foreach(_.cancel)
    timer = None

  /**
   * Should be called after any change to expression used in [[isTimerNecessary]].
   */
  def touchTimer(): Unit = timer match
    // Timer is running but it doesn't have to:
    case Some(task) if !isTimerNecessary => stopTimer()
    // Timer is not running but it should be:
    case None if isTimerNecessary => assignTimer()

  /**
   * Called each time the timer calls.
   */
  def timerCall(): Unit