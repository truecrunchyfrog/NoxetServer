package org.noxet.noxetserver.playerdata.types

import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.playerdata.PlayerDataType

class PDTInt extends PlayerDataType[Int]:
  override def getEmptyValue: Int = 0

  override def getValue(config: YamlConfiguration, key: String): Int =
    config.getInt(key)