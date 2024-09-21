package org.noxet.noxetserver.util

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.ChunkGenerator.{BiomeGrid, ChunkData}
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.messaging.{ClearChat, Message}
import org.noxet.noxetserver.playerdata.PlayerDataManager
import org.noxet.noxetserver.playerstate.PlayerState
import org.noxet.noxetserver.realm.RealmManager
import org.noxet.noxetserver.realm.RealmManager.goToHub
import org.noxet.noxetserver.util.Captcha.*

import java.util.Random
import scala.collection.mutable

class Captcha(player: Player, handler: CaptchaQuestion => Unit):
  private val assignedLocation = Location(
    getWorld,
    Random().nextInt(-500, 500),
    0,
    Random().nextInt(-500, 500))
  private var lastQuestion: Option[Int] = None
  private val bukkitTasks = mutable.ListBuffer[BukkitTask]()

  private var correctAnswer: Option[CaptchaSound] = None
  private var timeoutTask: Option[BukkitTask] = None

  stopPlayerCaptcha(player) // Stop any already existing captcha.
  captchaInstances.put(player, this)

  def getPlayer: Player = player

  def init(): Unit =
    RealmManager.setPlayerMigrationStatus(player, true) // Prevent migration manager from interacting with player.

    PlayerState.prepareIdle(player, true)

    getWorld.setBlockData(assignedLocation.clone().subtract(0, 1, 0), Material.BARRIER.createBlockData) // Place standing block.

    freezer.freeze(player, assignedLocation)

    player.sendTitle("§3§l" + TextBeautifier.beautify("noxet"), "§eCaptcha System", 0, 120, 0)

    ClearChat.send(player)
    Message("§eHello! Before we let you in, please do this little captcha test.").send(player)

    var delay = 5

    bukkitTasks.addOne(QuickRunnable(() =>
      ClearChat.send(player)
      Message("§bYou will hear a few sounds. Every time, you should answer by clicking the icon of what you heard.").send(player)
    ).runTaskLater(NoxetServer.getPlugin, 20 * delay))

    delay += 6

    bukkitTasks.addOne(QuickRunnable(() =>
      ClearChat.send(player)
      Message("§9§oUnable to hear sounds? Enable Minecraft Subtitles in the sound options.").send(player)
    ).runTaskLater(NoxetServer.getPlugin, 20 * delay))

    delay += 5

    bukkitTasks.addOne(QuickRunnable(askQuestion(0)).runTaskLater(NoxetServer.getPlugin, 20 * delay))

  private def askQuestion(index: Int): Unit =
    lastQuestion = Some(index)

    val captchaSounds = mutable.ArrayBuffer[CaptchaSound]()

    val random = Random()
    for _ <- 0 until answersPerQuestion do
      val remaining = CaptchaSound.values.diff(captchaSounds)
      captchaSounds.addOne(remaining(random.nextInt(remaining.length)))

    correctAnswer = Some(captchaSounds(random.nextInt(captchaSounds.size)))

    player.sendTitle("§d§lLISTEN", "§3What do you hear?", 0, 20 * 2, 0)

    player.playSound(player.getLocation, correctAnswer.get.sound, 1, 1)

    bukkitTasks.addOne(QuickRunnable(() =>
      handler(CaptchaQuestion(captchaSounds.toList, index + 1, chooseAnswer))

      ClearChat.send(player)
      Message("§eClick what sound you heard from the menu.").send(player)
    ).runTaskLater(NoxetServer.getPlugin, 20 * 2))

    timeoutTask = QuickRunnable(() =>
      stop()
      player.kickPlayer("§cYou did not respond in time.")
    ).runTaskLater(NoxetServer.getPlugin, 20 * 20)

  private def chooseAnswer(playerAnswer: CaptchaSound): Unit =
    correctAnswer match
      case Some(a) if a == playerAnswer =>
        // Answered correctly!

        correctAnswer = None

        ClearChat.send(player)

        timeoutTask.foreach(_.cancel)

        player.stopAllSounds()
        player.playSound(player.getLocation, Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.5f)

        if lastQuestion.get < totalQuestions - 1 then
          val waitSecs = 3
          for secondsLeft <- waitSecs until 0 by -1 do
            bukkitTasks.addOne(QuickRunnable(() =>
              player.sendTitle(
                "§2§lCORRECT",
                s"§a${if lastQuestion.get != totalQuestions - 2 then "Another" else "Last"} sound in §f$secondsLeft§as...",
                0, 40, 0)
            ).runTaskLater(NoxetServer.getPlugin, (waitSecs - secondsLeft) * 20))

          bukkitTasks.addOne(QuickRunnable(askQuestion(lastQuestion.get + 1))
            .runTaskLater(NoxetServer.getPlugin, waitSecs * 20))
        else
          finish()
      case None => Message("§cPlease wait!").send(player)
      case _ =>
        stop()
        player.kickPlayer("§cWrong answer! Try again.")

  def finish(): Unit =
    stop()

    Message("§aYou answered correctly and will now continue to the server.").send(player)

    player.sendTitle("§a§lDone", "§eYou seem human enough.", 0, 60, 0)

    PlayerDataManager(player).set(PlayerDataManager.Attribute.HasDoneCaptcha, true).save()

    QuickRunnable(() =>
      PlayerState.prepareDefault(player)
      if player.isOnline then
        goToHub(player)
    ).runTaskLater(NoxetServer.getPlugin, 60)

  def stop(): Unit =
    captchaInstances.remove(player)

    freezer.unfreeze(player)

    bukkitTasks.foreach(_.cancel())

    RealmManager.setPlayerMigrationStatus(player, false)

    getWorld.setBlockData(assignedLocation.clone.subtract(0, 1, 0), Material.AIR.createBlockData) // Remove standing block.

object Captcha:
  case class CaptchaQuestion(
                              captchaSounds: List[CaptchaSound],
                              soundIndex: Int,
                              callback: CaptchaSound => Unit)

  import org.bukkit.Material.*
  import org.bukkit.Sound.*

  enum CaptchaSound(val name: String, val sound: Sound, val material: Material):
    case Dog extends CaptchaSound("Wolf", ENTITY_WOLF_HURT, WOLF_SPAWN_EGG)
    case Pig extends CaptchaSound("Pig", ENTITY_PIG_AMBIENT, PIG_SPAWN_EGG)
    case Sheep extends CaptchaSound("Sheep", ENTITY_SHEEP_AMBIENT, SHEEP_SPAWN_EGG)
    case Creeper extends CaptchaSound("Creeper", ENTITY_CREEPER_PRIMED, CREEPER_HEAD)
    case Glass extends CaptchaSound("Glass", BLOCK_GLASS_BREAK, GLASS)
    case Explosion extends CaptchaSound("Explosion", ENTITY_GENERIC_EXPLODE, TNT)
    case Parrot extends CaptchaSound("Parrot", ENTITY_PARROT_AMBIENT, PARROT_SPAWN_EGG)
    case ShulkerBox extends CaptchaSound("Shulker Box", BLOCK_SHULKER_BOX_OPEN, SHULKER_BOX)
    case EnderDragon extends CaptchaSound("Ender Dragon", ENTITY_ENDER_DRAGON_GROWL, DRAGON_HEAD)
    case Wither extends CaptchaSound("Wither", ENTITY_WITHER_AMBIENT, WITHER_ROSE)
    case Chest extends CaptchaSound("Chest", BLOCK_CHEST_OPEN, CHEST)
    case Enderman extends CaptchaSound("Enderman", ENTITY_ENDERMAN_AMBIENT, ENDERMAN_SPAWN_EGG)
    case Silverfish extends CaptchaSound("Silverfish", ENTITY_SILVERFISH_AMBIENT, SILVERFISH_SPAWN_EGG)
    case Deepslate extends CaptchaSound("Deepslate", BLOCK_DEEPSLATE_BREAK, DEEPSLATE)
    case Fire extends CaptchaSound("Fire", BLOCK_FIRE_EXTINGUISH, FLINT_AND_STEEL)
    case Anvil extends CaptchaSound("Anvil", BLOCK_ANVIL_USE, ANVIL)
    case Horse extends CaptchaSound("Horse", ENTITY_HORSE_AMBIENT, HORSE_SPAWN_EGG)
    case Armor extends CaptchaSound("Armor", ITEM_ARMOR_EQUIP_GENERIC, ARMOR_STAND)
    case Grass extends CaptchaSound("Grass", BLOCK_GRASS_BREAK, GRASS_BLOCK)
    case EndPortal extends CaptchaSound("End Portal Frame", BLOCK_END_PORTAL_FRAME_FILL, END_PORTAL_FRAME)
    case NetherPortal extends CaptchaSound("Nether Portal", BLOCK_PORTAL_AMBIENT, OBSIDIAN)
    case Wool extends CaptchaSound("Wool", BLOCK_WOOL_BREAK, WHITE_WOOL)
    case Dolphin extends CaptchaSound("Dolphin", ENTITY_DOLPHIN_AMBIENT, DOLPHIN_SPAWN_EGG)
    case Burp extends CaptchaSound("Burping", ENTITY_PLAYER_BURP, COOKED_RABBIT)
    case Fall extends CaptchaSound("Fall", ENTITY_PLAYER_BIG_FALL, PLAYER_HEAD)
    case Zombie extends CaptchaSound("Zombie", ENTITY_ZOMBIE_AMBIENT, ZOMBIE_HEAD)
    case Skeleton extends CaptchaSound("Skeleton", ENTITY_SKELETON_AMBIENT, SKELETON_SKULL)
    case Villager extends CaptchaSound("Villager", ENTITY_VILLAGER_YES, EMERALD)

  val answersPerQuestion = 3
  val totalQuestions = 5

  private val captchaInstances: mutable.HashMap[Player, Captcha] = mutable.HashMap()
  private val freezer = PlayerFreezer(20)

  def getWorld: World =
    WorldCreator("noxet_void_world").generator(new ChunkGenerator:
      def generateChunkData(world: World, random: Random, x: Int, z: Int, biome: BiomeGrid): ChunkData = createChunkData(world)
    ).createWorld()

  private def getPlayerCaptcha(player: Player): Option[Captcha] = captchaInstances.get(player)

  def isPlayerDoingCaptcha(player: Player): Boolean = captchaInstances.contains(player)

  /**
   * Stops a player's captcha process, if active.
   *
   * @param player The player whose captcha to abort
   */
  def stopPlayerCaptcha(player: Player): Unit =
    getPlayerCaptcha(player).foreach(_.stop())