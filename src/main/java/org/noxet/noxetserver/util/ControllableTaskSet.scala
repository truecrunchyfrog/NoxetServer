package org.noxet.noxetserver.util

import org.bukkit.scheduler.BukkitTask

import scala.collection.mutable

class ControllableTaskSet:
  private val tasks: mutable.Set[BukkitTask] = mutable.HashSet()

  def push(task: BukkitTask): Unit = tasks.add(task)

  def abortAll(): Unit = tasks.foreach(_.cancel)
    