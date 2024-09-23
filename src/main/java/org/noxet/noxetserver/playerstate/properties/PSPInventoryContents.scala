package org.noxet.noxetserver.playerstate.properties

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.noxet.noxetserver.playerstate.PlayerStateProperty

import scala.jdk.CollectionConverters.*

object PSPInventoryContents extends PlayerStateProperty[Array[ItemStack]]:
  override def getConfigName: String = "inventory_contents"

  override def getDefaultSerializedProperty: Array[ItemStack] = Array.empty

  override def getSerializedPropertyFromPlayer(player: Player): Array[ItemStack] =
    player.getInventory.getContents

  override def restoreProperty(player: Player, contents: Array[ItemStack]): Unit =
    player.getInventory.setContents(contents)

  override def getValueFromConfig(config: ConfigurationSection): Array[ItemStack] =
    config.getList(getConfigName).asScala.toArray