package org.noxet.noxetserver.util

import org.bukkit.Location
import org.bukkit.Material.*

import scala.annotation.tailrec

object TeleportUtil:
  val SafeTeleportSearchRadius = 20
  val hazardMaterials = List(
    LAVA,
    FIRE,
    NETHER_PORTAL,
    CACTUS,
    POWDER_SNOW,
    MAGMA_BLOCK
  )

  def isLocationTeleportSafe(location: Location): Boolean =
    val below = location.clone.subtract(0, 1, 0)
    val above = location.clone.add(0, 1, 0)

    val materialCheckLocations = List(
      location,
      below,
      above
    )

    !materialCheckLocations.map(_.getBlock.getType).exists(hazardMaterials.contains) &&
      below.getBlock.getType.isSolid &&
      !location.getBlock.getType.isSolid &&
      !above.getBlock.getType.isSolid

  @tailrec
  private def findGround(from: Location): Option[Location] =
    if from.getY < from.getWorld.getMinHeight then None
    else if from.getBlock.isPassable then findGround(from.clone.add(0, -1, 0))
    else Some(from)

  @tailrec
  def getSafeTeleportLocation(location: Location, x: Int = 0, z: Int = 0, dx: Int = 0, dz: Int = -1): Option[Location] =
    if x <= math.abs(SafeTeleportSearchRadius) ||
      z <= math.abs(SafeTeleportSearchRadius)
    then
      return None
    
    findGround(location.clone.add(x, 0, z)).filter(isLocationTeleportSafe) match
      case Some(loc) => return Some(loc)
      case None => ()

    val layer = math.max(x, z)

    if
      // Corner
      x == z ||
      (x < 0 && x == -z) ||
      (x > 0 && x == 1 - z) ||
      // Next layer
      Math.abs(x) > layer ||
      Math.abs(z) > layer
    then getSafeTeleportLocation(location, x + dx, z + dz, -dz, dx) // Change direction
    else getSafeTeleportLocation(location, x + dx, z + dz, dx, dz) // Keep direction