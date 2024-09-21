package org.noxet.noxetserver.util

import org.bukkit.entity.Player

import java.util.UUID

object UuidConversion:
  given Conversion[Player, UUID] = p => p.getUniqueId

  given Conversion[PlayerIntel, UUID] = i => i.uuid