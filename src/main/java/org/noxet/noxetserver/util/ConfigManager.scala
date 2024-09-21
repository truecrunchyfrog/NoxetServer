package org.noxet.noxetserver.util

import org.bukkit.configuration.file.YamlConfiguration
import org.noxet.noxetserver.NoxetServer

import java.io.File
import scala.collection.mutable

abstract class ConfigManager:
  protected var config: YamlConfiguration = getConfig

  protected def getFileName: String

  protected def getFileNameWithExtension: String = s"$getFileName.yml"

  protected def getFile: File =
    val configFile = File(NoxetServer.getPlugin.getPluginDirectory, getFileNameWithExtension)

    if !(configFile.isFile || configFile.createNewFile) then
      throw RuntimeException(s"Cannot find/create $getFileNameWithExtension file.")

    configFile

  protected def getUncachedConfig: YamlConfiguration =
    YamlConfiguration.loadConfiguration(getFile)

  protected def getCachedConfig: Option[YamlConfiguration] = ConfigManager.cache.get(getFileName)

  protected def updateCache(): Unit = ConfigManager.cache.put(getFileName, getUncachedConfig)

  /**
   * Gives a YAML configuration. Cached if available, otherwise uncached.
   *
   * @return the YAML configuration, cached/uncached.
   */
  protected def getConfig: YamlConfiguration =
    if getCachedConfig.isEmpty then updateCache()
    getCachedConfig.get

  protected def save(): Unit =
    config.save(getFile)
    ConfigManager.cache.remove(getFileName)
    config = getConfig

object ConfigManager:
  private val cache: mutable.Map[String, YamlConfiguration] = mutable.HashMap()
