package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.noxet.noxetserver.playerstate.PlayerStateProperty

import scala.jdk.CollectionConverters.*

object PSPInventoryArmor extends PlayerStateProperty[Array[ItemStack]]:
  override def getConfigName: String = "armor"

  override def getDefaultSerializedProperty: Array[ItemStack] = Array.empty

  override def getSerializedPropertyFromPlayer(player: Player): Array[ItemStack] =
    player.getInventory.getArmorContents

  override def restoreProperty(player: Player, armor: Array[ItemStack]): Unit =
    player.getInventory.setArmorContents(armor)

  override def getValueFromConfig(config: ConfigurationSection): Array[ItemStack] =
    config.getList(getConfigName).asScala.toArray