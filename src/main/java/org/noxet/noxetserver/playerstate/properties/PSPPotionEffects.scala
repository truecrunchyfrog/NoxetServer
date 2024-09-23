package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.noxet.noxetserver.playerstate.PlayerStateProperty

import scala.jdk.CollectionConverters.*

object PSPPotionEffects extends PlayerStateProperty[Array[PotionEffect]]:
  override def getConfigName: String = "potion_effects"

  override def getDefaultSerializedProperty: Array[PotionEffect] = Array.empty

  override def getSerializedPropertyFromPlayer(player: Player): Array[PotionEffect] =
    player.getActivePotionEffects.asScala.toArray

  override def restoreProperty(player: Player, potionEffectList: Array[PotionEffect]): Unit =
    potionEffectList.foreach(player.addPotionEffect)

  override def getValueFromConfig(config: ConfigurationSection): Array[PotionEffect] =
    config.getList(getConfigName).asScala.toArray