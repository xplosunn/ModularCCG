package unit.common.network.messages

import java.util.Random

import client.window.tab.panel.GraphicalRemoteCard
import common.card.Summon
import common.game.{GameSteps, RemoteCard}
import common.network.messages.clientToServer.{ChatToServer, GameAction}
import org.junit.Assert.{assertEquals, assertTrue, fail}
import org.junit._
import unit.UnitTestConstants

/**
 * Created by HugoSousa on 22-10-2014.
 */
object MessagesToServerTest {
  val server: FakeServer = new FakeServer
  val fakeClient: FakeServerHandler = new FakeServerHandler("testUser")

  @BeforeClass def setup(){
    server.start()
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(true, fakeClient.connect._1)
    Thread.sleep(UnitTestConstants.processMillis)
  }

  @AfterClass def shutdown(){
    server.shutdown()
    Thread.sleep(UnitTestConstants.processMillis)
  }
}
class MessagesToServerTest {

  @Test def chatToServer() {
    MessagesToServerTest.fakeClient.Chat.sendMessage("hi there")
    Thread.sleep(UnitTestConstants.processMillis)
    MessagesToServerTest.server.handlers.get(0).recievedMessages.lastElement() match{
      case msg: ChatToServer =>
        assertTrue(msg.getMessage == "hi there")
        assertTrue(msg.getTargetType == ChatToServer.TARGET.ROOM)
        assertTrue(msg.getTarget == "main")
      case msg => fail("" + msg.getClass.getName)
    }
  }

  @Test def gameAction(){
    val gameID = new Random().nextInt()
    val randomStep = GameSteps.values()(new Random().nextInt(GameSteps.values().length))
    MessagesToServerTest.fakeClient.Duel.nextStep(gameID, randomStep)
    Thread.sleep(UnitTestConstants.processMillis)
    MessagesToServerTest.server.handlers.get(0).recievedMessages.lastElement() match{
      case msg: GameAction =>
        assertTrue(msg.getAction == GameAction.ACTIONS.NEXT_STEP)
        assertTrue(msg.getGameID == gameID)
        assertTrue(msg.getAttackerIDs == null)
        assertTrue(msg.getCardID == -1)
        assertTrue(msg.getDefenses == null)
        assertEquals(randomStep, msg.getCurrentStep)
      case msg => fail("" + msg.getClass.getName)
    }

    MessagesToServerTest.fakeClient.Duel.nextTurn(gameID)
    Thread.sleep(UnitTestConstants.processMillis)
    MessagesToServerTest.server.handlers.get(0).recievedMessages.lastElement() match{
      case msg: GameAction =>
        assertTrue(msg.getAction == GameAction.ACTIONS.END_TURN)
        assertTrue(msg.getGameID == gameID)
        assertTrue(msg.getAttackerIDs == null)
        assertTrue(msg.getCardID == -1)
        assertTrue(msg.getDefenses == null)
      case _ => fail()
    }

    val cardToPlay = new GraphicalRemoteCard(new RemoteCard(0, "MrTest", new Summon),null)
    MessagesToServerTest.fakeClient.Duel.playCard(gameID, cardToPlay)
    Thread.sleep(UnitTestConstants.processMillis)
    MessagesToServerTest.server.handlers.get(0).recievedMessages.lastElement() match{
      case msg: GameAction =>
        assertTrue(msg.getAction == GameAction.ACTIONS.PLAY_CARD)
        assertTrue(msg.getGameID == gameID)
        assertTrue(msg.getAttackerIDs == null)
        assertTrue(msg.getCardID == 0)
        assertTrue(msg.getDefenses == null)
      case _ => fail()
    }

    val attackerIDs = Array(new Random().nextInt(), new Random().nextInt(), new Random().nextInt())
    MessagesToServerTest.fakeClient.Duel.setAttackers(gameID, attackerIDs)
    Thread.sleep(UnitTestConstants.processMillis)
    MessagesToServerTest.server.handlers.get(0).recievedMessages.lastElement() match{
      case msg: GameAction =>
        assertTrue(msg.getAction == GameAction.ACTIONS.SET_ATTACKERS)
        assertTrue(msg.getGameID == gameID)
        assertTrue(msg.getAttackerIDs.size == attackerIDs.size)
        for(i <- msg.getAttackerIDs.indices)
          assertTrue(msg.getAttackerIDs()(i) == attackerIDs(i))
        assertTrue(msg.getCardID == -1)
        assertTrue(msg.getDefenses == null)
      case _ => fail()
    }

    val defenses = Array((0,1), (0,2),
      (3,4), (3,5), (3,6),
      (7,8))
    val defenseTuples = Array((0,1), (0,2),
      (3,4), (3,5), (3,6),
      (7,8))
    MessagesToServerTest.fakeClient.Duel.setDefenses(gameID, defenses)
    Thread.sleep(UnitTestConstants.processMillis)
    MessagesToServerTest.server.handlers.get(0).recievedMessages.lastElement() match {
      case msg: GameAction =>
        assertTrue(msg.getAction == GameAction.ACTIONS.SET_DEFENDERS)
        assertTrue(msg.getGameID == gameID)
        assertTrue(msg.getAttackerIDs == null)
        assertTrue(msg.getCardID == -1)
        assertTrue(msg.getDefenses.size == defenseTuples.size)
        for(i<- defenseTuples.indices){
          assertTrue(msg.getDefenses()(i)._1 == defenseTuples(i)._1)
          assertTrue(msg.getDefenses()(i)._2 == defenseTuples(i)._2)
        }
      case _ => fail()
    }
  }
}