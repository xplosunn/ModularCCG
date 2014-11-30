package common.card.ability.change

/**
 * Created by Hugo on 20-06-2014.
 */
class SummonValueChange(val summonID: Int, val change: Int, val value: Int) extends GameChange{
  override def toString =
    "SummonValueChange: Summon " + summonID + "'s value " + change + " is now " + value
}
