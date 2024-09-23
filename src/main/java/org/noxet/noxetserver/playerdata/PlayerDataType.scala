package org.noxet.noxetserver.playerdata

import org.bukkit.configuration.file.YamlConfiguration

trait PlayerDataType[T]:
  def getEmptyValue: T

  def getValue(config: YamlConfiguration, key: String): T