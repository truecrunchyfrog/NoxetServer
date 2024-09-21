package org.noxet.noxetserver.util

import scala.collection.mutable

object FancyTimeConverter:
    def deltaSecondsToFancyTime(seconds: Int, secondSpecific: Boolean = false): String =
        val days = seconds / 86400
        val hours = seconds / 3600 % 24
        val minutes = seconds / 60 % 60

        val parts = mutable.ListBuffer[String]()

        if days > 0 then
            parts += days + "d"

        if hours > 0 then
            parts += hours + "h"

        if days == 0 && minutes > 0 then
            parts += minutes + "m"

        if days == 0 && hours == 0 && (secondSpecific || minutes == 0) then
            parts += seconds % 60 + "s"

        parts.mkString(" ")