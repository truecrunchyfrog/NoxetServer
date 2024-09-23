package org.noxet.noxetserver.playerstate.properties

import org.bukkit.Statistic.Type.*
import org.bukkit.configuration.{ConfigurationSection, MemorySection}
import org.bukkit.entity.{EntityType, Player}
import org.bukkit.{Material, Statistic}
import org.noxet.noxetserver.playerstate.PlayerStateProperty
import org.noxet.noxetserver.playerstate.properties.PSPStatistics.safeGetMap

import scala.jdk.CollectionConverters.*

object PSPStatistics extends PlayerStateProperty[Map[String, Any]]:
  extension [K, V](map: Map[K, V])
    def safeGetMap[K2, V2](key: K): Map[K2, V2] = {
      map.get(key) match
        case memSec: MemorySection => memSec.getValues(true).asScala
        case normalMap => normalMap
    }.asInstanceOf[Map[K2, V2]]

  override def getConfigName: String = "statistics"

  override def getDefaultSerializedProperty: Map[String, Any] =
    Map(
      "untyped" -> Map[String, Int](),
      "material" -> Map[String, Map[String, Int]](),
      "entity" -> Map[String, Map[String, Integer]]()
    )

  override def getSerializedPropertyFromPlayer(player: Player): Map[String, Any] =
    val allStats = Statistic.values

    val untypedPlayerStats =
      allStats
        .filter(_.getType == UNTYPED)
        .map(stat => stat.name -> player.getStatistic(stat))
        .toMap

    val materialPlayerStats =
      allStats
        .filter(s => s.getType == BLOCK || s.getType == ITEM)
        .map(stat => stat.name ->
          Material.values
            .map(material => material.name -> player.getStatistic(stat, material))
            .filter(_._2 != 0) // exclude empty statistics, to prevent bloated files.
            .toMap)
        .toMap

    val entityPlayerStats =
      allStats
        .filter(_.getType == ENTITY)
        .map(stat => stat.name ->
          EntityType.values
            .map(entityType => entityType.name -> player.getStatistic(stat, entityType))
            .filter(_._2 != 0)
            .toMap)
        .toMap

    Map[String, Any](
      "untyped" -> untypedPlayerStats,
      "material" -> materialPlayerStats,
      "entity" -> entityPlayerStats
    )

  override def restoreProperty(player: Player, stats: Map[String, Any]): Unit =
    val untypedPlayerStats = stats.safeGetMap[String, Int]("untyped")
    val materialPlayerStats = stats.safeGetMap[String, Map[String, Int]]("material")
    val entityPlayerStats = stats.safeGetMap[String, Map[String, Int]]("entity")

    for stat <- Statistic.values do
      stat.getType match
        case UNTYPED =>
          player.setStatistic(stat, untypedPlayerStats.getOrElse(stat.name, 0))
        case BLOCK | ITEM =>
          if materialPlayerStats.contains(stat.name) then
            Material.values.foreach(material =>
              player.setStatistic(
                stat,
                material,
                materialPlayerStats.safeGetMap[String, Int](stat.name)
                  .getOrElse(material.name, 0)
              )
            )
        case ENTITY =>
          if entityPlayerStats.contains(stat.name) then
            EntityType.values.foreach(entityType =>
              player.setStatistic(
                stat,
                entityType,
                entityPlayerStats.safeGetMap[String, Int](stat.name)
                  .getOrElse(entityType.name, 0)
              )
            )

  override def getValueFromConfig(config: ConfigurationSection): Map[String, Any] =
    Option(config.get(getConfigName).asInstanceOf[MemorySection])
      .get
      .getValues(false)
      .asScala.toMap