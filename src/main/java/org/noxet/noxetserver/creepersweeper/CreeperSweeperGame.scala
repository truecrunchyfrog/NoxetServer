package org.noxet.noxetserver.creepersweeper

import org.noxet.noxetserver.util.InventoryCoordinate

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.util.Random

class CreeperSweeperGame(width: Int, height: Int, creepers: Int):
  private enum CreeperSweeperStatus:
    case NotStarted, Playing, Ended

  val tiles: HashMap[InventoryCoordinate, CreeperSweeperTile] =
    (0 until width * height).map(i => (InventoryCoordinate.fromSlotIndex(i), CreeperSweeperTile()))
  private var status = CreeperSweeperStatus.NotStarted
  private var hasWon = false
  private var gameStart: Option[Long] = None

  def start(startCoordinate: InventoryCoordinate): Unit =
    if status != CreeperSweeperStatus.NotStarted then return

    status = CreeperSweeperStatus.Playing

    assert(creepers <= width * height - 1)
    tiles.filterNot(x => x._1 == startCoordinate)
      .sampleMany(creepers)(Random()).get.foreach(_.creeper = true)

    gameStart = Some(System.currentTimeMillis)

  def stop(): Unit = status = CreeperSweeperStatus.Ended

  def getGameDuration: Long = System.currentTimeMillis - gameStart.getOrElse(0L)

  def isPlaying: Boolean = status == CreeperSweeperStatus.Playing

  def hasEnded: Boolean = status == CreeperSweeperStatus.Ended

  def didWin: Boolean = hasWon

  /**
   * Performs a recursive tile reveal.
   *
   * @param coordinate The tile to reveal
   */
  def revealTile(coordinate: InventoryCoordinate): Unit =
    if !isPlaying then
      start(coordinate)

    val tile = tiles(coordinate)

    if tile.flagged then return // Cannot reveal flagged tile!

    if tile.creeper then
      onRevealCreeperTile(coordinate)
      return

    tile.revealRecursively(this, coordinate, List())

    if !tiles.exists(x => !x._2.creeper && !x._2.revealed) then
      // If all non-creeper tiles are revealed.
      onFinish()

  def onRevealCreeperTile(coordinate: InventoryCoordinate): Unit = stop()

  def onFinish(): Unit =
    hasWon = true
    stop()

  def getNeighborTiles(coord: InventoryCoordinate): HashMap[InventoryCoordinate, CreeperSweeperTile] = {
    for
      dx <- 0 until 3
      dy <- 0 until 3
      if !(dx == 1 && dy == 1)
    yield
      val x = coord.x - 1 + dx
      val y = coord.y - 1 + dy
      if !(x >= 9 || x < 0) then // Only X is modulus-ed so we don't need to check Y for out of bounds.
        val neighborCoord = InventoryCoordinate(x, y)
        tiles.get(neighborCoord).map((neighborCoord, _))
  }.toSet.flatMap

  def countCreeperNeighbors(coordinate: InventoryCoordinate): Int =
    getNeighborTiles(coordinate).count(_._2.creeper)

  /**
   * Reveals a tile, and neighbor tiles, recursively.
   */
  def revealRecursively(coordinate: InventoryCoordinate, ignore: mutable.HashSet[InventoryCoordinate]): Unit =
    tiles(coordinate).revealed = true

    if countCreeperNeighbors(coordinate) != 0 then return

    ignore.add(coordinate)

    getNeighborTiles(coordinate)
      .filterNot(x => ignore.contains(x._1))
      .foreach(x => revealRecursively(x._1, ignore))