package org.noxet.noxetserver.playerdata.types

import org.bukkit.Location
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.playerdata.PlayerDataType

import scala.jdk.CollectionConverters.*

class PDTMapStringMapStringLocation extends PlayerDataType[Map[String, Map[String, Location]]]:
  override def getEmptyValue: Map[String, Map[String, Location]] = Map.empty

  override def getValue(config: YamlConfiguration, key: String): Map[String, Map[String, Location]] =
    Option(config.get(key).asInstanceOf[MemorySection]) match
      case Some(memSec) =>
        memSec.getValues(false).asScala.toMap
          .map((name, memSec) => name ->
            memSec.asInstanceOf[MemorySection]
              .getValues(false).asScala
              .asInstanceOf[Map[String, Location]])
      case None => Map.empty