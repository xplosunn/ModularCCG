package common.card.ability.change

/**
 * Created by Hugo on 20-06-2014.
 */
class ZoneChange(val cardID: Int, val targetZoneOwner: String, val targetZone: Int) extends GameChange{
  override def toString =
    "ZoneChange: common.card " + cardID + "is now in " + targetZoneOwner + "'s zone " + targetZone
}