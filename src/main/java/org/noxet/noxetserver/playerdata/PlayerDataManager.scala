package org.noxet.noxetserver.playerdata

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.playerdata.types.*

import java.io.{File, IOException, StringReader}
import java.util.UUID

class PlayerDataManager(uuid: UUID):
  private val config: YamlConfiguration = getConfig(uuid)

  def get(attribute: Attribute): Any =
    attribute.getType.getValue(config, attribute.getKey)

  def set(attribute: Attribute, value: Any): PlayerDataManager =
    config.set(attribute.getKey, value)
    updateCache(uuid.toUuid, config)
    this

  def toggleBoolean(attribute: Attribute): PlayerDataManager =
    set(attribute, !get(attribute).asInstanceOf[Boolean])

  def addInt(attribute: Attribute, addWith: Int): PlayerDataManager =
    set(attribute, get(attribute).asInstanceOf[Int] + addWith)

  def addLong(attribute: Attribute, addWith: Long): PlayerDataManager =
    set(attribute, get(attribute).asInstanceOf[Long] + addWith)

  def incrementInt(attribute: Attribute): PlayerDataManager = addInt(attribute, 1)

  def remove(attribute: Attribute): PlayerDataManager = set(attribute, null)

  def addToStringList(attribute: Attribute, value: String): PlayerDataManager =
    val list = config.getStringList(attribute.getKey)
    list.add(value)
    set(attribute, list)

  def removeFromStringList(attribute: Attribute, value: String): PlayerDataManager =
    val list = config.getStringList(attribute.getKey)
    list.remove(value)
    set(attribute, list)

  def doesContain(attribute: Attribute, value: Any): Boolean =
    Option(config.getList(attribute.getKey)) match
      case Some(list) => list.contains(value)
      case None => false

  def getListSize(attribute: Attribute): Int =
    Option(config.getList(attribute.getKey)) match
      case Some(list) => list.size
      case None => 0

  def save(): Unit = saveData(uuid.toUuid, config)

object PlayerDataManager:
  private val configCache: HashMap[UUID, String] = HashMap()

  enum Attribute(val kind: PlayerDataType[?]):
    case HasDoneCaptcha extends Attribute(PDTBoolean)
    case BlockedPlayers extends Attribute(PDTStringList)
    case MsgSpokenTo extends Attribute(PDTStringList)
    case MsgDisabled extends Attribute(PDTBoolean)
    case DisallowIncomingFriendRequests extends Attribute(PDTBoolean)
    case DisallowIncomingTpaRequests extends Attribute(PDTBoolean)
    case Homes extends Attribute(PDTMapStringMapStringLocation)
    case HasUnderstoodAnarchy extends Attribute(PDTBoolean)
    case SeenChatNotice extends Attribute(PDTBoolean)
    case Muted extends Attribute(PDTBoolean)
    case TimesJoined extends Attribute(PDTInteger)
    case SecondsPlayed extends Attribute(PDTInteger)
    case LastPlayed extends Attribute(PDTLong)
    case FriendList extends Attribute(PDTStringList)
    case IncomingFriendRequests extends Attribute(PDTStringList)
    case OutgoingFriendRequests extends Attribute(PDTStringList)
    case FriendTeleportation extends Attribute(PDTBoolean)
    case ShowFriendHomes extends Attribute(PDTBoolean)
    case CreeperSweeperWins extends Attribute(PDTInteger)
    case CreeperSweeperLosses extends Attribute(PDTInteger)
    case CreeperSweeperTotalWinPlaytime extends Attribute(PDTLong)
    case DisallowIncomingPartyInvites extends Attribute(PDTBoolean)

    def getKey: String = name.toLowerCase

  private def getDirectory: File =
    val playerDataDir = File(NoxetServer.getPlugin.getPluginDirectory, "PlayerData")

    if !playerDataDir.mkdir && (!playerDataDir.exists || !playerDataDir.isDirectory) then
      throw RuntimeException("Cannot create PlayerData directory.")

    playerDataDir

  private def getDataFile(uuid: UUID): File = File(getDirectory, s"$uuid.yml")

  def deleteDataFile(uuid: UUID): Unit =
    getDataFile(uuid).delete()
    clearCacheForUUID(uuid)

  private def updateCache(uuid: UUID, config: YamlConfiguration): Unit =
    if configCache.size > 50 then
      configCache.clear()
    configCache.put(uuid, config.saveToString)

  def clearCacheForUUID(uuid: UUID): Unit = configCache.remove(uuid)

  def clearAllCache(): Unit =
    configCache.clear()

  private def getConfig(uuid: UUID): YamlConfiguration =
    if configCache.containsKey(uuid) then
      return YamlConfiguration.loadConfiguration(StringReader(configCache.get(uuid)))

    val dataFile = getDataFile(uuid)

    if !dataFile.exists then
      return YamlConfiguration()

    val config = YamlConfiguration.loadConfiguration(dataFile)

    updateCache(uuid, config)

    config

  private def saveData(uuid: UUID, config: YamlConfiguration): Unit =
    config.save(getDataFile(uuid))