* migrate functions that use a Player when only a UUID is needed, to just use a UUID instead. this because of the given conversion.
* given Conversion[String, Option[Player]]
* given Conversion[UUID, Option[PlayerIntel]]
* given Conversion[String, Option[PlayerIntel]]