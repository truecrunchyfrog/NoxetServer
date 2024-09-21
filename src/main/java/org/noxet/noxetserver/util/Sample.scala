package org.noxet.noxetserver.util

import scala.util.Random

object Sample:
  extension[A] (seq: Seq[A])
    def sampleOne(using random: Random): Option[A] =
      if seq.nonEmpty then
        Some(seq(random.nextInt(seq.size)))
      else
        None

    def sampleMany(amount: Int)(using random: Random): Option[Seq[A]] =
      if seq.size >= amount then
        Some(random.shuffle(seq).take(amount))
      else
        None