package org.noxet.noxetserver.util

import org.bukkit.Location
import org.bukkit.entity.Player
import org.noxet.noxetserver.messaging.Message

import scala.collection.immutable.HashSet

class RegionBinder(center: Location, playerSet: HashSet[Player], width: Int, tickFrequency: Int) extends DynamicTimer:
    touchTimer()

    override def isTimerNecessary: Boolean = playerSet.nonEmpty

    override def getTickFrequency: Int = tickFrequency

    override def timerCall(): Unit =
        for
            player <- playerSet
            mobile = player.getLocation
            delta = center.clone.subtract(mobile)
            if Math.abs(delta.getX) > width || Math.abs(delta.getZ) > width
        do
            mobile.setX(center.getX + width * -Math.signum(delta.getX))
            mobile.setZ(center.getZ + width * -Math.signum(delta.getZ))
            player.teleport(mobile)
            Message("§cNuh uh!").send(player)