package org.noxet.noxetserver.util

case class InventoryCoordinate(x: Int, y: Int):
    def slotIndex: Int = y * 9 + x

object InventoryCoordinate:
    given Conversion[(Int, Int), InventoryCoordinate] = (x, y) => InventoryCoordinate(x, y)
    given Conversion[Int, InventoryCoordinate] = (slotIndex) => InventoryCoordinate(slotIndex)

    def apply(slotIndex: Int): InventoryCoordinate =
        InventoryCoordinate(slotIndex % 9, slotIndex / 9)