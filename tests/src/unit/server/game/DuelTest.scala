package unit.server.game

import java.util.concurrent.ConcurrentHashMap

import common.card.{Summon, Deck}
import common.game.GameSteps
import common.network.messages.clientToServer.GameAction
import org.junit.{After, Before, Test}
import org.junit.Assert._
import server.game.Duel
import server.services.Games
import unit.UnitTestConstants

import scala.collection.mutable.ArrayBuffer
import java.util.Random

object DuelTest{
  def duels = {
    val duelsAttempt = Games.getClass.getDeclaredMethod("server$services$Games$$duels").invoke(Games)
    duelsAttempt match {
      case d: ConcurrentHashMap[Int, Duel] => d
      case _ => fail(); null
    }
  }
}

class DuelTest {
  val c1: FakeClientHandler = new FakeClientHandler("testPlayer1")
  val c2: FakeClientHandler = new FakeClientHandler("testPlayer2")
  val deck: Deck = new Deck

  @Before
  def before(){
    deck.add(new Summon, 30)
    assertTrue(deck.validate)
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
    Games.newDuel(c1, c2, deck, deck)
    assertFalse(DuelTest.duels.isEmpty)
    DuelTest.duels.clear()
  }

  @Test
  def defendersTime(){
    val testDuel = new FakeDuel(c1, c2, deck, deck)
    testDuel.start()
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => testDuel.mulligan(p.handler.getUserName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => assertTrue(p.hand.size <= 4))
    assertTrue(testDuel.isAlive)

    val ap = testDuel.getGameState.activePlayer
    testDuel.addCardToBePlayed(ap.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(1, ap.battlefield.size)
    testDuel.endTurn()
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.addCardToBePlayed(testDuel.getGameState.activePlayer.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.endTurn()
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.nextStep()
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.setAttackers(Array(ap.battlefield(0).id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(GameSteps.COMBAT_Defend, testDuel.currentTurn.currentStep)
    Thread.sleep(Duel.SECONDS_TO_CHOOSE_DEFENDERS * 1000)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(GameSteps.MAIN_2nd, testDuel.currentTurn.currentStep)
    assertEquals(29, testDuel.getGameState.nonActivePlayer.lifeTotal)
  }

  @Test
  def disconnect(){
    //disconnect from a duel, wait 'til end of turn + process time, check if duel ended (thread should no longer be alive)
    val testDuel = new FakeDuel(c1, c2, deck, deck)
    testDuel.start()
    val waitTime = Duel.SECONDS_PER_TURN / 4 + new Random().nextInt(Duel.SECONDS_PER_TURN/4)
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => testDuel.mulligan(p.handler.getUserName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => assertTrue(p.hand.size <= 4))
    assertTrue(testDuel.isAlive)
    Thread.sleep(waitTime * 1000)
    testDuel.playerDisconnected()
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.secondsLeft < Duel.SECONDS_PER_TURN * 3/4 + 1)
    Thread.sleep((Duel.SECONDS_PER_TURN - waitTime)*1000)
    Thread.sleep(UnitTestConstants.processMillis *4)
    assertTrue(!testDuel.isAlive)
  }

  @Test
  def turnReaminingTime(){
    // wait some random time during the turn, do an action, then wait the time remaining for that turn, then check if it ended
    assertTrue(DuelTest.duels.isEmpty)
    val testDuel = new FakeDuel(c1, c2, deck, deck)
    testDuel.start()
    val waitTime = Duel.SECONDS_PER_TURN / 4 + new Random().nextInt(Duel.SECONDS_PER_TURN/4)
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => testDuel.mulligan(p.handler.getUserName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => assertTrue(p.hand.size <= 4))
    assertTrue(testDuel.isAlive)
    Thread.sleep(waitTime * 1000)
    val ap = testDuel.getGameState.activePlayer
    testDuel.addCardToBePlayed(ap.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(!ap.battlefield.isEmpty)
    assertTrue(""+testDuel.currentTurn.secondsLeft,testDuel.currentTurn.secondsLeft < Duel.SECONDS_PER_TURN *3/4)
    Thread.sleep((Duel.SECONDS_PER_TURN - waitTime)*1000)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.secondsLeft+" "+testDuel.currentTurn.currentStep, testDuel.getGameState.activePlayer != ap)
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
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => testDuel.mulligan(p.handler.getUserName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    //turn 1
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 4)

    testDuel.addCardToBePlayed(testDuel.getGameState.activePlayer.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.size == 1)
    Thread.sleep(UnitTestConstants.processMillis)
    testDuel nextStep()
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))

    testDuel.endTurn()
    //turn 2
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 4)

    val ap2 = testDuel.getGameState.activePlayer
    testDuel.endTurn()
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 3
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 4)
    assertFalse(ap2.equals(testDuel.getGameState.activePlayer))

    testDuel.nextStep()
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    val attackerID = testDuel.getGameState.activePlayer.battlefield(0).id
    testDuel.setAttackers(Array(attackerID))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    assertTrue(testDuel.getGameState.nonActivePlayer.lifeTotal == 29)
    testDuel.endTurn()
    //turn 4
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 5)
    testDuel.nextStep()
    Thread.sleep(UnitTestConstants.processMillis)

    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    testDuel.addCardToBePlayed(testDuel.getGameState.activePlayer.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.size == 1)
    testDuel.endTurn()
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 5
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 5)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    testDuel.nextStep()
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    testDuel.setAttackers(Array(attackerID))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Defend))
    val defenderID = testDuel.getGameState.nonActivePlayer.battlefield(0).id
    testDuel.setDefenses(Array((attackerID, defenderID)))

    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p =>{
      assertTrue(p.battlefield.size == 0)
      assertTrue(p.pile.size == 1)
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
    Thread.sleep(UnitTestConstants.processMillis)
    val hand1 = testDuel.getGameState.activePlayer.hand
    val hand2 = testDuel.getGameState.nonActivePlayer.hand
    p1handler.recieveMessage(GameAction.mulligan(testDuel.id, Array(hand1(0).id, hand1(1).id, hand1(2).id)))
    p2handler.recieveMessage(GameAction.mulligan(testDuel.id, Array(hand2(0).id, hand2(1).id, hand2(2).id)))

    //turn 1
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 4)

    p2handler.recieveMessage(GameAction.playCard(testDuel.id, testDuel.getGameState.activePlayer.hand(0).id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.size == 1)
    Thread.sleep(UnitTestConstants.processMillis)
    p2handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))

    p2handler.recieveMessage(GameAction.endTurn(testDuel.id))
    //turn 2
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 4)

    val ap2 = testDuel.getGameState.activePlayer
    p1handler.recieveMessage(GameAction.endTurn(testDuel.id))
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 3
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 4)
    assertFalse(ap2.equals(testDuel.getGameState.activePlayer))

    p2handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    val attackerID = testDuel.getGameState.activePlayer.battlefield(0).id
    p2handler.recieveMessage(GameAction.setAttackers(testDuel.id, Array(attackerID)))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    assertTrue(testDuel.getGameState.nonActivePlayer.lifeTotal == 29)
    p2handler.recieveMessage(GameAction.endTurn(testDuel.id))
    //turn 4
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 5)
    p1handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(UnitTestConstants.processMillis)

    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_2nd))
    p1handler.recieveMessage(GameAction.playCard(testDuel.id, testDuel.getGameState.activePlayer.hand(0).id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.getGameState.activePlayer.battlefield.size == 1)
    p1handler.recieveMessage(GameAction.endTurn(testDuel.id))
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 5
    assertTrue(testDuel.getGameState.activePlayer.hand.size == 5)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    p2handler.recieveMessage(GameAction.endStep(testDuel.id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Attack))

    p2handler.recieveMessage(GameAction.setAttackers(testDuel.id, Array(attackerID)))
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.COMBAT_Defend))
    val defenderID = testDuel.getGameState.nonActivePlayer.battlefield(0).id
    testDuel.setDefenses(Array((attackerID, defenderID)))

    Thread.sleep(UnitTestConstants.processMillis)
    testDuel.getGameState.players.foreach(p => {
      assertTrue(p.battlefield.size == 0)
      assertTrue(p.pile.size == 1)
    })

    DuelTest.duels.remove(testDuel.id)
  }

  @Test
  def timeOuts() {
    assertTrue(DuelTest.duels.isEmpty)
    val testDuel = new FakeDuel(c1, c2, deck, deck)

    assertTrue(testDuel.getGameState.players.forall(p => p.hand.size + p.deck.cards.size == 30))
    testDuel.start()
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.HAND_SELECTION))
    Thread.sleep(UnitTestConstants.processMillis)
    Thread.sleep(Duel.SECONDS_TO_MULLIGAN * 1000)
    assertTrue(testDuel.getGameState.players.forall(p => p.hand.size + p.deck.cards.size == 30))
    assertTrue(testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))

    val ap = testDuel.getGameState.activePlayer
    Thread.sleep(Duel.SECONDS_PER_TURN * 1000)
    assertTrue(""+testDuel.currentTurn.currentStep,testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertNotEquals(ap, testDuel.getGameState.activePlayer)
    Thread.sleep(Duel.SECONDS_PER_TURN * 1000)
    Thread.sleep(UnitTestConstants.processMillis * 4)
    assertTrue(""+testDuel.currentTurn.currentStep, testDuel.currentTurn.currentStep.equals(GameSteps.MAIN_1st))
    assertEquals(ap, testDuel.getGameState.activePlayer)
  }
}