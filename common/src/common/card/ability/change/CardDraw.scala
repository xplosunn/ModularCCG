package common.card.ability.change

import common.game.RemoteCard

class CardDraw(val drawingPlayer: String, val card: RemoteCard, val deckEnded: Boolean) extends GameChange{
  override def toString =
    "CardDraw : " + drawingPlayer + " drawing " + card + "."
}