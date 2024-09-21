package org.noxet.noxetserver.realm

import org.bukkit.Location
import org.noxet.noxetserver.util.ConfigManager
import org.noxet.noxetserver.realm.RealmManager.Realm

object RealmDataManager extends ConfigManager:
  override protected def getFileName: String = "realm-data"

  private def getRealmKey(realm: Realm, key: String): String =
    val realmName =
      if realm != null then realm.name()
      else "_"
    realmName + "." + key

  def setSoftSpawnLocation(realm: Realm, location: Option[Location]): Unit =
    location match
      case Some(loc) =>
        loc.setX(Math.round(loc.getX * 2) / 2.0)
        loc.setY(Math.round(loc.getY * 2) / 2.0)
        loc.setZ(Math.round(loc.getZ * 2) / 2.0)

        loc.setYaw(Math.round(Math.abs(loc.getYaw) / 90) * 90 * Math.signum(loc.getYaw))
        loc.setPitch(Math.round(Math.abs(loc.getPitch) / 90) * 90 * Math.signum(loc.getPitch))
      case None => ()

    config.set(getRealmKey(realm, "spawn"), location.orNull)
    save()

  def getSpawnLocation(realm: Realm): Option[Location] =
    Option(config.getLocation(getRealmKey(realm, "spawn")))