package org.noxet.noxetserver.playerdata.types

import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.playerdata.PlayerDataType

class PDTBoolean extends PlayerDataType[Boolean]:
  override def getEmptyValue: Boolean = false

  override def getValue(config: YamlConfiguration, key: String): Boolean =
    config.getBoolean(key)