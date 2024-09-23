package org.noxet.noxetserver.playerdata.types

import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.playerdata.PlayerDataType

import scala.jdk.CollectionConverters.*

class PDTStringList extends PlayerDataType[List[String]]:
  override def getEmptyValue: List[String] = Nil

  override def getValue(config: YamlConfiguration, key: String): List[String] =
    config.getStringList(key).asScala.toList