package org.noxet.noxetserver.util

object FancyTimeConverter:
    def deltaSecondsToFancyTime(seconds: Int): String = deltaSecondsToFancyTime(seconds, false)

    def deltaSecondsToFancyTime(seconds: Int, secondSpecific: Boolean): String =
        val days = seconds / 86400
        val hours = seconds / 3600 % 24
        val minutes = seconds / 60 % 60

        val stringBuilder = StringBuilder()

        if days > 0 then
            stringBuilder.append(days).append("d ")

        if hours > 0 then
            stringBuilder.append(hours).append("h ")

        if days == 0 && minutes > 0 then
            stringBuilder.append(minutes).append("m ")

        if days == 0 && hours == 0 && (secondSpecific || minutes == 0) then
            stringBuilder.append(seconds % 60).append("s ")

        stringBuilder
          .deleteCharAt(stringBuilder.length() - 1)
          .toString()