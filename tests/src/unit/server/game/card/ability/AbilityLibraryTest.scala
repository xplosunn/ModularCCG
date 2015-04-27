package unit.server.game.card.ability

import common.card.{Summon, Deck}
import common.game.GameSteps
import common.network.messages.clientToServer.GameAction
import org.junit.Assert._
import org.junit.{After, Before}
import server.game.Player
import unit.UnitTestConstants
import unit.server.game.{DuelTest, FakeDuel, FakeClientHandler}

/**
 * Created by HugoSousa on 06-10-2014.
 */
abstract class AbilityLibraryTest {
  val processMillis = UnitTestConstants.processMillis
  var c1: FakeClientHandler = null
  var c2: FakeClientHandler = null
  var deck: Deck = null
  var ap: Player = null
  var nap: Player = null
  var testDuel: FakeDuel = null

  @Before
  def before(){
    c1 = new FakeClientHandler("weeOne")
    c2 = new FakeClientHandler("weeTwo")
    deck = new Deck
    deck.add(new Summon, 30)
    assertTrue(deck.validate)
    assertTrue(DuelTest.duels.isEmpty)
    testDuel = new FakeDuel(c1, c2, deck, deck)
    DuelTest.duels.put(testDuel.id, testDuel)
    testDuel.start()
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.HAND_SELECTION))


    //assertEquals(testDuel.getGameState.activePlayer.handler.getUserName, ap.handler.getUserName)

    testDuel.getGameState.players.foreach(p => testDuel.mulligan(p.handler.userName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    //assertEquals(testDuel.getGameState.activePlayer.handler.getUserName, ap.handler.getUserName)

    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    //assertEquals(testDuel.getGameState.activePlayer.handler.getUserName, ap.handler.getUserName)

    ap = testDuel.getGameState.activePlayer
    nap = testDuel.getGameState.nonActivePlayer

    assertEquals(4, ap.hand.size)
    assertEquals(3, nap.hand.size)
  }

  @After
  def after(){
    DuelTest.duels.remove(testDuel.id)
    assertTrue(DuelTest.duels.isEmpty)
    c1.logout()
    c2.logout()
  }

}
