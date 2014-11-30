package unit.common.network.messages

import java.util.Random

import common.card.Summon
import common.card.ability.change.GameChange
import common.game.{GameSteps, RemoteCard}
import common.network.messages.serverToClient.{ChatToClient, GameInfo}
import org.junit.Assert._
import org.junit.{AfterClass, BeforeClass, Test}
import server.ClientHandler
import unit.UnitTestConstants

object MessagesToClientTest {
  val server: FakeServer = new FakeServer
  val fakeClient: FakeServerHandler = new FakeServerHandler("testyTest")
  var handler: ClientHandler = null

  @BeforeClass def setup() {
    server.start()
    Thread.sleep(UnitTestConstants.processMillis)
    assertEquals(true, fakeClient.connect._1)
    Thread.sleep(UnitTestConstants.processMillis)
    handler = server.handlers.get(0)
  }

  @AfterClass def shutdown(){
    server.shutdown()
    Thread.sleep(UnitTestConstants.processMillis)
  }
}
class MessagesToClientTest {
  @Test def chatToClient() {
    MessagesToClientTest.handler.sendMessageToClient(new ChatToClient("sender", "message"))
    Thread.sleep(UnitTestConstants.processMillis)
    var msgs = MessagesToClientTest.fakeClient.recievedMessages
    var lastMsg = msgs.get(msgs.size - 1)
    lastMsg match {
      case msg: ChatToClient =>
        assertTrue(msg.getMessage == "message")
        assertTrue(msg.getRoom == null)
        assertTrue(msg.getSender == "sender")
      case _ => fail()
    }

    MessagesToClientTest.handler.sendMessageToClient(new ChatToClient("sender","room", "message"))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: ChatToClient =>
        assertTrue(msg.getMessage == "message")
        assertTrue(msg.getRoom == "room")
        assertTrue(msg.getSender == "sender")
      case _ => fail()
    }
  }

  @Test def gameInfo() {
    val gameID = new Random().nextInt()
    //Game started
    val playerNames = Array("PlayerOne","PlayerTwo")
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.gameStarted(gameID, playerNames))
    Thread.sleep(UnitTestConstants.processMillis)
    var msgs = MessagesToClientTest.fakeClient.recievedMessages
    var lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.GAME_STARTED)
        assertTrue(msg.playerNames().size == playerNames.size)
        for(i<- playerNames.indices)
          assertTrue(msg.playerNames()(i) == playerNames(i))
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes == null)
      case _ => fail()
    }

    //Next step
    val step = GameSteps.values()(new Random().nextInt(GameSteps.values().size))
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.nextStep(gameID, step))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.NEXT_STEP)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == step)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes == null)
      case _ => fail()
    }

    //Play card
    val remoteCard = new RemoteCard(0, "PlayerOne", new Summon)
    val changes: Array[GameChange] = Array()
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.cardPlayed(gameID, remoteCard, changes))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.CARD_PLAYED)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card.id == remoteCard.id)
        assertTrue(msg.card.owner == remoteCard.owner)
        assertTrue(msg.card.card == remoteCard.card)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes.size == changes.size)
        for(i<-changes.indices)
          assertTrue(msg.changes()(i) == changes(i))
      case _ => fail()
    }

    //Next turn
    val player = "PlayerOne" + new Random().nextLong()
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.nextTurn(gameID, player, changes))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.NEXT_TURN)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == player)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes.size == changes.size)
        for(i<-changes.indices)
          assertTrue(msg.changes()(i) == changes(i))
      case _ => fail()
    }

    //Game change
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.gameChanges(gameID, changes))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.GAME_CHANGES)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes.size == changes.size)
        for(i<-changes.indices)
          assertTrue(msg.changes()(i) == changes(i))
      case _ => fail()
    }

    //Player won
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.playerWon(gameID, player))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.PLAYER_WON)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == player)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes == null)
      case _ => fail()
    }

    //Attackers
    val attackerIDs = Array(7,2,13)
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.attackers(gameID, attackerIDs))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.ATTACKERS)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs.size == attackerIDs.size)
        for(i<-msg.attackerIDs.indices)
          assertTrue(msg.attackerIDs()(i) == attackerIDs(i))
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == null)
        assertTrue(msg.changes == null)
      case _ => fail()
    }

    //Defenders
    val defenseIDs = Array(Array(7,1,4),Array(2,3,6),Array(13,5,8))
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.defenders(gameID, defenseIDs))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match{
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.DEFENDERS)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs().size == defenseIDs.size)
        for(i<-msg.defenseIDs.indices){
          assertTrue(msg.defenseIDs()(i).size == defenseIDs(i).size)
          for(j<-msg.defenseIDs()(i).indices)
            assertTrue(msg.defenseIDs()(i)(j) == defenseIDs(i)(j))
        }

        assertTrue(msg.summonID == null)
        assertTrue(msg.changes == null)
      case _ => fail()
    }

    //On death ability
    val summonID = new Random().nextInt()
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.onDeathAbility(gameID, summonID, changes))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match {
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.ON_DEATH_ABILITY)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == summonID)
        assertTrue(msg.changes.size == changes.size)
        for (i <- changes.indices)
          assertTrue(msg.changes()(i) == changes(i))
      case _ => fail()
    }

    //On combat ability
    MessagesToClientTest.handler.sendMessageToClient(GameInfo.onCombatAbility(gameID, summonID, changes))
    Thread.sleep(UnitTestConstants.processMillis)
    msgs = MessagesToClientTest.fakeClient.recievedMessages
    lastMsg = msgs.get(msgs.size - 1)
    lastMsg match {
      case msg: GameInfo =>
        assertTrue(msg.gameID == gameID)
        assertTrue(msg.`type` == GameInfo.TYPES.ON_COMBAT_ABILITY)
        assertTrue(msg.playerNames == null)
        assertTrue(msg.step == null)
        assertTrue(msg.card == null)
        assertTrue(msg.player == null)
        assertTrue(msg.attackerIDs == null)
        assertTrue(msg.defenseIDs == null)
        assertTrue(msg.summonID == summonID)
        assertTrue(msg.changes.size == changes.size)
        for (i <- changes.indices)
          assertTrue(msg.changes()(i) == changes(i))
      case _ => fail()
    }
  }
}
