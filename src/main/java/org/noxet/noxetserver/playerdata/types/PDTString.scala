package org.noxet.noxetserver.playerdata.types

import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.playerdata.PlayerDataType

class PDTString extends PlayerDataType[String]:
  override def getEmptyValue: String = null

  override def getValue(config: YamlConfiguration, key: String): String =
    config.getString(key)