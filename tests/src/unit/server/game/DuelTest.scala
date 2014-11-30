package unit.server.game

import java.util.concurrent.ConcurrentHashMap

import common.card.{Summon, Deck}
import common.game.GameSteps
import common.network.messages.clientToServer.GameAction
import org.junit.{After, Before, Test}
import org.junit.Assert._
import server.game.{Duel, Player}
import server.services.Games
import unit.UnitTestConstants

import scala.collection.mutable.ArrayBuffer

object DuelTest{
  def duels = {
    val duelsField = Games.getClass.getDeclaredField("duels")
    if(duelsField == null) fail()
    duelsField.setAccessible(true)
    val duels = duelsField.get(Games) match {
      case d: ConcurrentHashMap[Int, Duel] => d
      case _ => fail(); null
    }
    duels
  }
}

class DuelTest {
  val processMillis = UnitTestConstants.processMillis

  var c1: FakeClientHandler = null
  var c2: FakeClientHandler = null
  var deck: Deck = null

  var inc = 0

  @Before
  def before(){
    c1 = new FakeClientHandler("testPlayer" + inc)
    inc += 1
    c2 = new FakeClientHandler("testPlayer" + inc)
    inc += 1
    deck = new Deck
    deck.add(new Summon, 30)
    assertTrue(DuelTest.duels.isEmpty)
  }

  @After
  def after(){
    assertTrue(DuelTest.duels.isEmpty)
    c1.logout()
    c2.logout()
  }

  @Test
  def newDuel() {
    Games.newDuel(c1,c2, deck, deck)
    assertFalse(DuelTest.duels.isEmpty)
    DuelTest.duels.clear()
  }

  @Test
  def nextGameID(){
    assertTrue(DuelTest.duels.isEmpty)
    val testDuel = new FakeDuel(c1, c2, deck, deck)
    val idsFound = new ArrayBuffer[Int]
    testDuel.getGameState.players.foreach(p =>
      p.deck.cards.foreach(c => {
        assertTrue(c.id+"",!idsFound.contains(c.id))
        idsFound += c.id
      }))
  }

  @Test
  def fullFiveTurnSequence(){
    assertTrue(DuelTest.duels.isEmpty)
    val testDuel = new FakeDuel(c1, c2, deck, deck)
    DuelTest.duels.put(testDuel.id, testDuel)

    testDuel.start()
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.HAND_SELECTION))
    //Mulligans
    Thread.sleep(processMillis)
    testDuel.getGameState.players.foreach(p => testDuel.mulligan(p.handler.getUserName, Array(p.hand.cards(0).id, p.hand.cards(1).id, p.hand.cards(2).id)))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    //turn 1
    Thread.sleep(processMillis)
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 4)

    testDuel.addCardToBePlayed(testDuel.getGameState.activePlayer.hand.cards(0).id)
    Thread.sleep(processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.summons.size == 1)
    Thread.sleep(processMillis)
    testDuel nextStep()
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))

    testDuel.endTurn()
    //turn 2
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 4)

    val ap2 = testDuel.getGameState.activePlayer
    testDuel.endTurn()
    Thread.sleep(processMillis)
    //turn 3
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 4)
    assertFalse(ap2.equals(testDuel.getGameState.activePlayer))

    testDuel.nextStep()
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    val attackerID = testDuel.getGameState.activePlayer.battlefield.summons(0).id
    testDuel.setAttackers(Array(attackerID))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    assertTrue(testDuel.getGameState.nonActivePlayer.lifeTotal == 29)
    testDuel.endTurn()
    //turn 4
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 5)
    testDuel.nextStep()
    Thread.sleep(processMillis)

    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    testDuel.addCardToBePlayed(testDuel.getGameState.activePlayer.hand.cards(0).id)
    Thread.sleep(processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.summons.size == 1)
    testDuel.endTurn()
    Thread.sleep(processMillis)
    //turn 5
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 5)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    testDuel.nextStep()
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    testDuel.setAttackers(Array(attackerID))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Defend))
    val defenderID = testDuel.getGameState.nonActivePlayer.battlefield.summons(0).id
    testDuel.setDefenses(Array((attackerID.asInstanceOf[Integer], defenderID.asInstanceOf[Integer])))

    Thread.sleep(processMillis)
    testDuel.getGameState.players.foreach(p =>{
      assertTrue(p.battlefield.cards.size == 0)
      assertTrue(p.pile.cards.size == 1)
    })

    DuelTest.duels.remove(testDuel.id)
  }

  @Test
  def fullFiveTurnSequenceWithMessages(){
    assertTrue(DuelTest.duels.isEmpty)
    val testDuel = new FakeDuel(c1, c2, deck, deck)
    DuelTest.duels.put(testDuel.id, testDuel)

    var p1handler: FakeClientHandler = null
    var p2handler: FakeClientHandler = null
    if(testDuel.getGameState.activePlayer.handler == c1){
      p1handler = c1
      p2handler = c2
    }else if(testDuel.getGameState.activePlayer.handler == c2){
      p1handler = c2
      p2handler = c1
    }else fail()

    testDuel.start()
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.HAND_SELECTION))
    //Mulligans
    Thread.sleep(processMillis)
    val hand1 = testDuel.getGameState.activePlayer.hand.cards
    val hand2 = testDuel.getGameState.nonActivePlayer.hand.cards
    p1handler.recieveMessage(GameAction.mulligan(testDuel.id, Array(hand1(0).id, hand1(1).id, hand1(2).id)))
    p2handler.recieveMessage(GameAction.mulligan(testDuel.id, Array(hand2(0).id, hand2(1).id, hand2(2).id)))

    //turn 1
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 4)

    p2handler.recieveMessage(GameAction.playCard(testDuel.id, testDuel.getGameState.activePlayer.hand.cards(0).id))
    Thread.sleep(processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.summons.size == 1)
    Thread.sleep(processMillis)
    p2handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))

    p2handler.recieveMessage(GameAction.endTurn(testDuel.id))
    //turn 2
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 4)

    val ap2 = testDuel.getGameState.activePlayer
    p1handler.recieveMessage(GameAction.endTurn(testDuel.id))
    Thread.sleep(processMillis)
    //turn 3
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 4)
    assertFalse(ap2.equals(testDuel.getGameState.activePlayer))

    p2handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    val attackerID = testDuel.getGameState.activePlayer.battlefield.summons(0).id
    p2handler.recieveMessage(GameAction.setAttackers(testDuel.id, Array(attackerID)))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    assertTrue(testDuel.getGameState.nonActivePlayer.lifeTotal == 29)
    p2handler.recieveMessage(GameAction.endTurn(testDuel.id))
    //turn 4
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 5)
    p1handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(processMillis)

    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    p1handler.recieveMessage(GameAction.playCard(testDuel.id, testDuel.getGameState.activePlayer.hand.cards(0).id))
    Thread.sleep(processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.summons.size == 1)
    p1handler.recieveMessage(GameAction.endTurn(testDuel.id))
    Thread.sleep(processMillis)
    //turn 5
    assertTrue(testDuel.getGameState.activePlayer.hand.cards.size == 5)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    p2handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    p2handler.recieveMessage(GameAction.setAttackers(testDuel.id, Array(attackerID)))
    Thread.sleep(processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Defend))
    val defenderID = testDuel.getGameState.nonActivePlayer.battlefield.summons(0).id
    testDuel.setDefenses(Array((attackerID.asInstanceOf[Integer], defenderID.asInstanceOf[Integer])))

    Thread.sleep(processMillis)
    testDuel.getGameState.players.foreach(p => {
      assertTrue(p.battlefield.cards.size == 0)
      assertTrue(p.pile.cards.size == 1)
    })

    DuelTest.duels.remove(testDuel.id)
  }

  @Test
  def timeOuts() {
    assertTrue(DuelTest.duels.isEmpty)
    val testDuel = new FakeDuel(c1, c2, deck, deck)

    assertTrue(testDuel.getGameState.players.forall(p => p.hand.cards.size + p.deck.cards.size == 30))
    testDuel.start()
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.HAND_SELECTION))
    Thread.sleep(processMillis)
    Thread.sleep(Duel.SECONDS_TO_MULLIGAN * 1000)
    assertTrue(testDuel.getGameState.players.forall(p => p.hand.cards.size + p.deck.cards.size == 30))
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))

    val ap = testDuel.getGameState.activePlayer
    Thread.sleep(Duel.SECONDS_PER_TURN * 1000)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertNotEquals(ap, testDuel.getGameState.activePlayer)
    Thread.sleep(Duel.SECONDS_PER_TURN * 1000)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertEquals(ap, testDuel.getGameState.activePlayer)
  }

}
