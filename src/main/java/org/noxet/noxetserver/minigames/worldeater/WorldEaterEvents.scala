package org.noxet.noxetserver.minigames.worldeater

import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.potion.{PotionEffect, PotionEffectType}
import org.noxet.noxetserver.messaging.Message
import org.noxet.noxetserver.minigames.MiniGameController
import org.noxet.noxetserver.util.{EntityGlowColor, Promise}

import java.util.Random

object WorldEaterEvents:
  enum GameEvent(eventName: String, eventConsumer: (WorldEater, Promise) => Unit):
    case HOT_SUN( "Hot sun"
    , WorldEaterEvents :: hotSun
    )
    case QUICK_STOVE( "Quick stove"
    , WorldEaterEvents :: quickStove
    )
    case METEOR_RAIN( "Meteor rain"
    , WorldEaterEvents :: meteorRain
    )
    case VISIBLE_HIDERS( "Exposed hiders"
    , WorldEaterEvents :: visibleHiders
    )
    case LOOT_DROP( "Loot drop"
    , WorldEaterEvents :: lootDrop
    )
    case DRILLING( "Drilling"
    , WorldEaterEvents :: drilling
    )
    case EXPLODING_HORSES( "Exploding horses"
    , WorldEaterEvents :: explodingHorses
    )
    case EVERYONE_VISIBLE( "Everyone exposed"
    , WorldEaterEvents :: everyoneVisible
    )

  def hotSun(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1, 2)
    worldEater.sendGameMessage(
      Message("§6§lHOT SUN! §6The sun is on fire! Get in shelter, or you will too..."))

    worldEater.scheduleTaskTimer(task =>
      if promise.isReported then
        task.cancel()

      worldEater.getPlayers
        .filter(p => MiniGameController.getMiniGameWorld.getHighestBlockYAt(p.getLocation) < p.getLocation.getY)
        .foreach(p => p.setFireTicks(30))
      , 20 * 5, 20)

    worldEater.scheduleTask(promise.report(), 20 * 60 * 2)

  def quickStove(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ITEM_GOAT_HORN_PLAY, 1, 2)
    worldEater.sendGameMessage(
      Message("§e§lQUICK STOVE! §eFurnaces will now not only DUPE what's cooked, " +
        "it will also cook at FIVE TIMES (5x) the speed! Get on your grill now, " +
        "because this will only last for 2 minutes."))

    worldEater.scheduleTask(promise.report(), 20 * 60 * 2)

  def meteorRain(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_3, 1, 2)
    worldEater.sendGameMessage(Message(
      "§c§lMETEOR RAIN! §cHead to shelter!\nWhen you hear a horn, a meteor is flying towards you. "))

    val random = Random()

    val meteorAmount = 10
    for i <- 0 until meteorAmount do
      val isLast = i == meteorAmount - 1
      worldEater.scheduleTask(() =>
        val meteorTarget = worldEater.getRandomPlayer(true)

        meteorTarget.playSound(meteorTarget, Sound.ITEM_GOAT_HORN_SOUND_0, 1, 0.5f)
        worldEater.sendGameMessage(Message(s"§5§l§k### ### ### ### §c${meteorTarget.getName}§4 is targeted by a meteor."))

        val targetLocation = meteorTarget.getLocation

        val meteorStart = targetLocation.clone
        meteorStart.add(random.nextInt(-50, 50), random.nextInt(30, 100), random.nextInt(-50, 50))

        val meteor = MiniGameController.getMiniGameWorld.spawn(meteorStart, Fireball.`type`)

        EntityGlowColor.setGlowColor(meteor, ChatColor.RED)
        meteor.setGlowing(true)
        meteor.setIsIncendiary(true)
        meteor.setYield(8)

        meteor.setDirection(targetLocation.toVector.subtract(meteorStart.toVector))

        if isLast then
          promise.report()
      , 20 * (i + 1) * 15)

  def visibleHiders(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.8f)
    worldEater.sendGameMessage(Message(
      "§c§lALERT! §eHiders are now visible for 10 seconds!"))

    worldEater.getTeamSet.forEach(WorldEaterTeams.Hider, hider =>
      hider.sendTitle("§c§lEXPOSED!", "§eYour location is now visible.", 5, 20 * 10, 5)
      hider.addPotionEffect(
        PotionEffect(
          PotionEffectType.GLOWING, 20 * 10, 10, true, false, false
    )))

    worldEater.scheduleTask(promise.report(), 20 * 10)

  def lootDrop(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ENTITY_CAMEL_SADDLE, 1, 0.5f)
    worldEater.sendGameMessage(Message(
      "§9§lLOOT DROP! §9Look up for a falling loot box, with only the best to offer."))
    val random = Random()

    val dropLocation = worldEater.getCenterChunk.getBlock(
      random.nextInt(16),
      0,
      random.nextInt(16)
    ).getLocation

    dropLocation.setY(
      MiniGameController.getMiniGameWorld.getHighestBlockYAt(dropLocation) + 100)

    val fallingLootBox = MiniGameController.getMiniGameWorld.spawnFallingBlock(dropLocation, Material.BARREL.createBlockData)

    EntityGlowColor.setGlowColor(fallingLootBox, ChatColor.BLUE)
    fallingLootBox.setGlowing(true)
    fallingLootBox.setDropItem(false)

    worldEater.scheduleTaskTimer(task =>
      // TODO ??? isReported already?
      if promise.isReported || fallingLootBox.isDead then
        promise.report()
        task.cancel()
      , 120, 20)

    worldEater.scheduleTask(promise.report(), 20 * 100)

  def drilling(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_4, 1, 2f)
    worldEater.sendGameMessage(Message(
      "§c§lDRILLING! §cDrills will now be performed randomly. " +
        "A whole Y-axis will be drilled down into void!"))
    val random = Random()

    val drillHoles = 15

    for i <- 0 until drillHoles do
      val isLast = i == drillHoles - 1

      worldEater.scheduleTask(() =>
        val drillLocation = worldEater.getCenterChunk.getBlock(
          random.nextInt(0, 16),
          0,
          random.nextInt(0, 16)
        ).getLocation

        val yMax = MiniGameController.getMiniGameWorld.getMaxHeight
        val yMin = MiniGameController.getMiniGameWorld.getMinHeight

        for y <- yMin until yMax do
          val drillBlock = drillLocation.clone
          drillBlock.setY(y)

          MiniGameController.getMiniGameWorld.spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 3)
          val isLast2 = isLast && y == yMin + 1

          worldEater.scheduleTask(() =>
            if y % 2 == 0 then
              MiniGameController.getMiniGameWorld.playSound(drillBlock, Sound.BLOCK_BAMBOO_BREAK, 1, 2)

            MiniGameController.getMiniGameWorld.spawnParticle(Particle.SWEEP_ATTACK, drillBlock, 5)
            drillBlock.getBlock.setBlockData(Material.AIR.createBlockData, false)

            if isLast2 then
              promise.report()
            , 2 * (yMax - y))
        , 20 * 15 * (i + 1))

  def explodingHorses(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.sendGameMessage(Message(
      "§8<§k-§8> §4§lSUDDEN DEATH! §cExploding horses will appear. They may " +
        "be killed with a single hit, but - if not - they will put you down."))

    worldEater.playGameSound(Sound.ENTITY_HORSE_ANGRY, 5, 5)

    val horseCount = 20

    for i <- 0 until horseCount do
      val isLast = i == horseCount - 1

      worldEater.scheduleTask(() =>
        if Math.random() < .3 then
          val unluckyPlayer = worldEater.getRandomPlayer
          unluckyPlayer.playSound(unluckyPlayer, Sound.ENTITY_HORSE_ANGRY, 6, 6)

          val horse = MiniGameController.getMiniGameWorld.spawnEntity(
            unluckyPlayer.getLocation, EntityType.HORSE).asInstanceOf[Horse]

          horse.setVisualFire(true)
          horse.setHealth(0.5)

          worldEater.scheduleTask(() =>
            if !horse.isDead then
              MiniGameController.getMiniGameWorld.playSound(horse.getLocation, Sound.ENTITY_GHAST_SCREAM, 1, 0.5f)
              horse.remove()
              MiniGameController.getMiniGameWorld.createExplosion(horse.getLocation, 12)

            if isLast then
              promise.report()
            , 20 * 4)
        , 20 * 3 * i)

  def everyoneVisible(worldEater: WorldEater, promise: Promise): Unit =
    worldEater.playGameSound(Sound.ITEM_GOAT_HORN_SOUND_7, 1, 0.5f)
    worldEater.sendGameMessage(Message(
      "§c§lALERT! §eEVERYONE are now visible!"))

    worldEater.getPlayers.foreach(player =>
      player.sendTitle("§c§lEXPOSED!", "§eEveryone can now see everyone.", 5, 20 * 5, 5)
      player.addPotionEffect(
        PotionEffect(
          PotionEffectType.GLOWING,
          20 * 60,
          10,
          ambient: true,
          particles: false,
          icon: false
        )))

    worldEater.scheduleTask(promise.report(), 20 * 60)