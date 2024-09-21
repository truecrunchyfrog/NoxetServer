package org.noxet.noxetserver.util

import org.bukkit.scheduler.BukkitRunnable

object QuickRunnable:
  def apply(f: => Unit): BukkitRunnable = () => f