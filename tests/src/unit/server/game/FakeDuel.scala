package unit.server.game

import common.card.Deck
import server.ClientHandler
import server.game.{Player, Duel}

/**
 * Created by xs on 04-10-2014.
 */
class FakeDuel(p1h: ClientHandler, p2h: ClientHandler, p1d: Deck, p2d: Deck) extends Duel(p1h, p2h, p1d, p2d) {
  def getGameState = gameState
  def currentTurn = CurrentTurn
}
