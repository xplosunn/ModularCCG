package server.game.card

import common.card.{Summon, Spell, Card}
import common.game.RemoteCard
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

  def remoteCard = new RemoteCard(id, owner.handler.userName, card)

  override def toString: String =
    "GameCard: card->[" + card + "],owner->[" + owner + "],id->" + id
}
case class GameSpell(override val card: Spell, o: Player, i: Int) extends GameCard(card,o, i)

case class GameSummon(override val card: Summon, o: Player, i: Int) extends GameCard(card, o, i)