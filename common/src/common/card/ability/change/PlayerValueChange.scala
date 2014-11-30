package common.card.ability.change

/**
 * Created by Hugo on 20-06-2014.
 */
class PlayerValueChange(val playerName: String, val changeValueIdentifier: Int, val effectiveNewValue: Int) extends GameChange{
  override def toString =
    "PlayerValueChange: " + playerName + "'s value " + changeValueIdentifier + " is now " + effectiveNewValue
}