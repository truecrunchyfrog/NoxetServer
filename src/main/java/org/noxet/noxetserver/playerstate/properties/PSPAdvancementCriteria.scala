package org.noxet.noxetserver.playerstate.properties

import org.bukkit.advancement.{Advancement, AdvancementProgress}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.playerstate.PlayerState.getAdvancementCriteriaList
import org.noxet.noxetserver.playerstate.PlayerStateProperty

import scala.jdk.CollectionConverters.*

object PSPAdvancementCriteria extends PlayerStateProperty[Array[String]]:
  override def getConfigName: String = "advancement_criteria"

  override def getDefaultSerializedProperty: Array[String] = Array.empty

  override def getSerializedPropertyFromPlayer(player: Player): Array[String] =
    getAdvancementCriteriaList(player).toArray

  override def restoreProperty(player: Player, restoreCriteriaList: Array[String]): Unit =
    NoxetServer.getPlugin.getServer.advancementIterator
      .map(player.getAdvancementProgress)
      .foreach(p =>
        p.getRemainingCriteria.asScala.toList
          .intersect(restoreCriteriaList)
          .foreach(p.awardCriteria)

        p.getAwardedCriteria.asScala.toList
          .filter(_.intersect(restoreCriteriaList).isEmpty)
          .foreach(p.revokeCriteria)
      )

  override def getValueFromConfig(config: ConfigurationSection): Array[String] =
    config.getStringList(getConfigName).asScala.toArray