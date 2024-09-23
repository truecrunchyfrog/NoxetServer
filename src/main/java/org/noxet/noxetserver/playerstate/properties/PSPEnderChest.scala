package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.noxet.noxetserver.playerstate.PlayerStateProperty

import scala.jdk.CollectionConverters.*

object PSPEnderChest extends PlayerStateProperty[Array[ItemStack]]:
  override def getConfigName: String = "ender_chest"

  override def getDefaultSerializedProperty: Array[ItemStack] = Array.empty

  override def getSerializedPropertyFromPlayer(player: Player): Array[ItemStack] =
    player.getEnderChest.getContents

  override def restoreProperty(player: Player, chestContents: Array[ItemStack]): Unit =
    player.getEnderChest.setContents(chestContents)

  override def getValueFromConfig(config: ConfigurationSection): Array[ItemStack] =
    config.getList(getConfigName).asScala.toArray