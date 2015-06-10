package unit.server.game

import java.util.Random

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestFSMRef, TestActorRef}
import common.card.{Summon, Deck}
import common.game.GameSteps
import org.junit.Assert._
import org.junit.{After, Before, Test}
import server.game._
import server.game.card.{GameSummon, BattlefieldSummon}
import unit.UnitTestConstants

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
    implicit val actorSystem = ActorSystem("HelloSystem")
    val actor: TestActorRef[fsmDuel] = TestFSMRef(new fsmDuel())
    actor ! NewDuel(gs)
    Thread.sleep(UnitTestConstants.processMillis)
    (actor, gs)
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

  @Test
  def defendersTime(){
    val (fsm, gameState) = newFsmDuel
    fsm.start()
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => fsm ! HandSelected(p.handler.userName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => assertTrue(p.hand.size <= 4 && p.hand.size >= 3))

    def ap = gameState.activePlayer
    assertEquals(Main_1, fsm.underlyingActor.stateName)
    fsm ! PlayCard(ap.handler.userName, ap.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(1, ap.battlefield.size)
    fsm ! EndTurn(ap.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    fsm ! PlayCard(ap.handler.userName, ap.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    fsm ! EndTurn(ap.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    fsm ! NextStep(ap.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    fsm ! SetAttackers(ap.handler.userName, Array(ap.battlefield(0).id))
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Combat_Defend, fsm.underlyingActor.stateName)
    Thread.sleep(Duel.SECONDS_TO_CHOOSE_DEFENDERS * 1000)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Main_2, fsm.underlyingActor.stateName)
    assertEquals(29, gameState.nonActivePlayer.lifeTotal)
  }

  @Test
  def turnReaminingTime() {
    // wait some random time during the turn, do an action, then wait the time remaining for that turn, then check if it ended
    val (duelActor, gameState) = newFsmDuel
    duelActor.start()
    Thread.sleep(UnitTestConstants.processMillis)
    val waitTime = Duel.SECONDS_PER_TURN / 4 + new Random().nextInt(Duel.SECONDS_PER_TURN/4)
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => duelActor ! HandSelected(p.handler.userName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => assertTrue(p.hand.size <= 4))
    Thread.sleep(waitTime * 1000)
    val ap = gameState.activePlayer
    duelActor ! PlayCard(ap.handler.userName, ap.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(ap.battlefield.nonEmpty)
    Thread.sleep((Duel.SECONDS_PER_TURN - waitTime)*1000)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(gameState.activePlayer != ap)
  }

  @Test
  def fullFiveTurnSequence(){
    val (duelActor, gameState) = newFsmDuel

    duelActor.start()
    assertEquals(HandSelection, duelActor.underlyingActor.stateName)
    //Mulligans
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => duelActor ! HandSelected(p.handler.userName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 1
    assertEquals(Main_1, duelActor.underlyingActor.stateName)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(4, gameState.activePlayer.hand.size)

    duelActor ! PlayCard(gameState.activePlayer.handler.userName, gameState.activePlayer.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(1, gameState.activePlayer.battlefield.size)
    assertEquals(3, gameState.activePlayer.hand.size)
    Thread.sleep(UnitTestConstants.processMillis)
    duelActor ! NextStep(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Main_2, duelActor.underlyingActor.stateName)
    val ap = gameState.activePlayer
    duelActor ! EndTurn(gameState.activePlayer.handler.userName)
    //turn 2
    Thread.sleep(UnitTestConstants.processMillis)
    assertNotEquals(ap, gameState.activePlayer)
    assertEquals(Main_1, duelActor.underlyingActor.stateName)
    assertEquals(4, gameState.activePlayer.hand.size)

    val ap2 = gameState.activePlayer
    duelActor ! EndTurn(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 3
    assertEquals(Main_1, duelActor.underlyingActor.stateName)
    assertEquals(4, gameState.activePlayer.hand.size)
    assertNotEquals(ap2, gameState.activePlayer)

    duelActor ! NextStep(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Combat_Attack, duelActor.underlyingActor.stateName)

    val attackerID = gameState.activePlayer.battlefield(0).id
    duelActor ! SetAttackers(gameState.activePlayer.handler.userName, Array(attackerID))
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Main_2, duelActor.underlyingActor.stateName)
    assertEquals(29, gameState.nonActivePlayer.lifeTotal)
    duelActor ! EndTurn(gameState.activePlayer.handler.userName)
    //turn 4
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Main_1, duelActor.underlyingActor.stateName)
    assertEquals(5, gameState.activePlayer.hand.size)
    duelActor ! NextStep(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)

    assertEquals(Main_2, duelActor.underlyingActor.stateName)
    duelActor ! PlayCard(gameState.activePlayer.handler.userName, gameState.activePlayer.hand(0).id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(1, gameState.activePlayer.battlefield.size)
    duelActor ! EndTurn(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    //turn 5
    assertEquals(5, gameState.activePlayer.hand.size)
    assertEquals(Main_1, duelActor.underlyingActor.stateName)
    duelActor ! NextStep(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Combat_Attack, duelActor.underlyingActor.stateName)

    duelActor ! SetAttackers(gameState.activePlayer.handler.userName, Array(attackerID))
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(Combat_Defend, duelActor.underlyingActor.stateName)
    val defenderID = gameState.nonActivePlayer.battlefield(0).id
    duelActor ! SetDefenders(gameState.activePlayer.handler.userName, Array((attackerID, defenderID)))

    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p =>{
      assertTrue(p.battlefield.size == 0)
      assertTrue(p.pile.size == 1)
    })

  }

  @Test
  def attackForWin(){
    val (duelActor, gameState) = newFsmDuel

    duelActor.start()
    assertEquals(HandSelection, duelActor.underlyingActor.stateName)
    //Mulligans
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => duelActor ! HandSelected(p.handler.userName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)

    assertEquals(Main_1, duelActor.underlyingActor.stateName)
    gameState.activePlayer.battlefield += new BattlefieldSummon(new GameSummon(new Summon{power(30)}, gameState.activePlayer, 300))
    duelActor ! NextStep(gameState.activePlayer.handler.userName)
    Thread.sleep(UnitTestConstants.processMillis)

    assertEquals(Combat_Attack, duelActor.underlyingActor.stateName)

    duelActor ! SetAttackers(gameState.activePlayer.handler.userName, Array(300))

    Thread.sleep(UnitTestConstants.processMillis)

    assertEquals(0, gameState.nonActivePlayer.lifeTotal)
    assertEquals(Available, duelActor.underlyingActor.stateName)
  }

  @Test
  def unExpectedMessage(){
    val (duelActor, gameState) = newFsmDuel
    duelActor.start()
    assertEquals(HandSelection, duelActor.underlyingActor.stateName)

    Thread.sleep(UnitTestConstants.processMillis)
    duelActor ! EndTurn(gameState.activePlayer.handler.userName)
    //Mulligans
    Thread.sleep(UnitTestConstants.processMillis)
    gameState.players.foreach(p => duelActor ! HandSelected(p.handler.userName, Array(p.hand(0).id, p.hand(1).id, p.hand(2).id)))
    Thread.sleep(UnitTestConstants.processMillis)

    assertEquals(Main_1, duelActor.underlyingActor.stateName)
  }
}
