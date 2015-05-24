package unit.server.game

import akka.actor.{Props, ActorSystem}
import common.card.{Summon, Deck}
import org.junit.Assert._
import org.junit.{After, Before, Test}
import server.game.{Player, GameState, fsmDuel}

import scala.collection.mutable.ArrayBuffer


/**
 * Created by xs on 24-05-2015.
 */
class fsmDuelTest {
  val c1: FakeClientHandler = new FakeClientHandler("testPlayerAnna")
  val c2: FakeClientHandler = new FakeClientHandler("testPlayerBoyScout")
  val deck: Deck = new Deck

  var _nextCardID = 0

  def nextCardID(): Int = {
    synchronized{
      _nextCardID += 1
      _nextCardID
    }
  }

  def newFsmDuel = {
    val gs = new GameState(Array(new Player(deck, c1, nextCardID),new Player(deck, c2, nextCardID)), nextCardID)
    (ActorSystem("HelloSystem").actorOf(Props(new fsmDuel(gs))), gs)
  }

  @Before
  def before(){
    deck.add(new Summon, 30)
    assertTrue(deck.validate)
    assertTrue(DuelTest.duels.isEmpty)
  }

  @After
  def after(){
    c1.logout()
    c2.logout()
  }

  @Test
  def newDuel() {
    val (actor, gameState) = newFsmDuel
    assertNotNull(actor)
    assertNotNull(gameState)
  }

  @Test
  def nextGameID(){
    val (_, gameState) = newFsmDuel
    val idsFound = new ArrayBuffer[Int]
    gameState.players.foreach(p =>
      p.deck.cards.foreach(c => {
        assertTrue(c.id+"",!idsFound.contains(c.id))
        idsFound += c.id
      }))
  }


}
