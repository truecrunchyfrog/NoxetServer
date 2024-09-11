package org.noxet.noxetserver.combatlogging

import org.bukkit.entity.Player
import org.noxet.noxetserver.playerstate.PlayerState
import org.noxet.noxetserver.realm.RealmManager.Realm
import org.noxet.noxetserver.util.ConfigManager

class CombatLoggingStorageManager extends ConfigManager:
  override def getFileName = "combat-logging"

  def setPlayerCombatLogMode(player: Player, realm: Realm, combatLogged: Boolean): Unit =
    val key = (
      if realm != null then realm.getPlayerStateType
      else PlayerState.PlayerStateType.GLOBAL
    ).name

    val realmCombatList = config.getStringList(key)

    if combatLogged then
      realmCombatList.add(player.getUniqueId.toString)
    else
      realmCombatList.remove(player.getUniqueId.toString)

    config.set(key, realmCombatList)
    save()

  def isCombatLogged(player: Player, realm: Realm): Boolean =
    val key = (
      if realm != null then realm.getPlayerStateType
      else PlayerState.PlayerStateType.GLOBAL
    ).name
    config.getStringList(key).contains(player.getUniqueId.toString)

  def triggerRejoin(player: Player, realm: Realm): Unit =
    if isCombatLogged(player, realm) then // Check if player left while combat logged last time.
      setPlayerCombatLogMode(player, realm, false) // Reset combat log state.
      player.setHealth(0) // Kill player!