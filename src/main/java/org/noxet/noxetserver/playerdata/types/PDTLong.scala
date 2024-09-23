package org.noxet.noxetserver.playerdata.types

import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.playerdata.PlayerDataType

class PDTLong extends PlayerDataType[Long]:
  override def getEmptyValue: Long = 0

  override def getValue(config: YamlConfiguration, key: String): Long =
    config.getLong(key)