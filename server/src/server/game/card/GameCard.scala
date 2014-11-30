package server.game.card

import common.card.{Summon, Spell, Card}
import server.game.Player

/**
 * Created by Hugo on 08-06-2014.
 */
object GameCard{
  def build(card: Card, owner: Player, id: Int) = card match {
    case card: Summon => new GameSummon(card, owner, id)
    case card: Spell => new GameSpell(card, owner, id)
  }
}
abstract class GameCard(val card: Card, val owner: Player, val id: Int) {

  def name = card.name

  def cost = card.cost

  override def toString: String =
    "GameCard: card->[" + card + "],owner->[" + owner + "],id->" + id
}
case class GameSpell(override val card: Spell, o: Player, i: Int) extends GameCard(card,o, i){}

case class GameSummon(override val card: Summon, o: Player, i: Int) extends GameCard(card, o, i){

  var power: Int = card.power
  var life: Int = card.life
  def originalPower = card.power
  def originalLife = card.life

  def restoreOriginalPowerAndLife() {
    power = card.power
    life = card.life
  }

  def changePowerBy(value: Int) {
    power += value
    if (power < 0) power = 0
  }

  def changeLifeBy(value: Int) {
    life += value
    if (life < 0) life = 0
  }

  def setZeroLife() {
    life = 0
  }

}