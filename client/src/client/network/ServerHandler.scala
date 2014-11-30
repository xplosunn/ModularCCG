package client.network

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}
import java.net.{ConnectException, Socket}

import client.ClientSession
import client.game.Games
import client.window.tab.panel.GraphicalRemoteCard
import client.window.tab.{DuelTab, LobbyTab}
import common.card.Deck
import common.network.NetworkValues
import common.network.messages.clientToServer.{ChatToServer, GameAction, LoginRequest, RequestToServer}
import common.network.messages.serverToClient._

import scala.collection.mutable.ArrayBuffer
import scala.swing.Dialog

class ServerHandler(val username: String) extends Thread {
  private var socket: Socket = null
  private var objIn: ObjectInputStream = null
  private var objOut: ObjectOutputStream = null
  private val responsesWaiting = new ArrayBuffer[(Int,(ResponseToClient)=>Unit)]
  private var connected = false

  def connect: (Boolean,String) = {
    try{
      socket = new Socket(NetworkValues.SERVER_IP, NetworkValues.SERVER_PORT)
      objOut = new ObjectOutputStream(socket.getOutputStream)
      objIn = new ObjectInputStream(socket.getInputStream)
      objOut.writeObject(new LoginRequest(username))
      val loginResponse: LoginResponse = objIn.readObject().asInstanceOf[LoginResponse]
      loginResponse.getType match{
        case LoginResponse.TYPE.SUCCESS =>
          connected = true
          start()
          (true, "")

        case LoginResponse.TYPE.ALREADY_LOGGED =>
          (false, "Already logged on another machine.")
      }
    }catch{
      case e: ConnectException => (false, "Could not connect to server.")
      case e: IOException => (false, "Could not connect to server.")
    }
  }

  override def run(){
    try {
      requestJoinChat("main")
      while (connected) {
        val msg = objIn.readObject().asInstanceOf[MessageToClient]
        handleMessage(msg)
      }
    }catch{
      case e: IOException => connected = false; ClientSession.disconnect()
    }
  }

  protected def handleMessage(message: MessageToClient){
    message match {
      case msg: InfoToClient =>
        msg.getType match{
        case InfoToClient.TYPE.CHAT_USER_JOINED =>
          LobbyTab.addChatUser(msg.getOrigin, msg.getData)
        }

      case msg: ChatToClient =>
        LobbyTab.addChatMessage(msg.getRoom ,msg.getSender,msg.getMessage)

      case msg: ResponseToClient =>
        for(i<-responsesWaiting.indices){
          if(msg.getID == responsesWaiting(i)._1){
            responsesWaiting(i)._2(msg)
            responsesWaiting.synchronized(()=>{responsesWaiting -= responsesWaiting(i)})
            return
          }
        }

      case info: GameInfo =>
        info.`type` match{
          case GameInfo.TYPES.GAME_STARTED=>
            val gamePanel = new DuelTab(ClientSession.SessionVars.serverHandler.username,info.gameID,info.playerNames)
            val window = ClientSession.SessionVars.mainWindow
            if(window != null){
              window.addGameTab(info.gameID, gamePanel)
              Games.newDuel(gamePanel)
            }
          case GameInfo.TYPES.HAND_PRE_MULLIGAN =>
            Games.handPreMulligan(info.gameID, info.getCards)
          case GameInfo.TYPES.HAND =>
            Games.hand(info.gameID, info.getCardIDs)

          case GameInfo.TYPES.CARD_PLAYED=>
            Games.cardPlayed(info.gameID,info.card,info.changes)

          case GameInfo.TYPES.GAME_CHANGES=>
            Games.applyGameChanges(info.gameID,info.changes)

          case GameInfo.TYPES.ATTACKERS =>
            Games.attackersAnnounced(info.gameID, info.attackerIDs)
          case GameInfo.TYPES.DEFENDERS =>
          case GameInfo.TYPES.ON_COMBAT_ABILITY => Games.combatAbility(info.gameID, info.summonID(), info.changes())
          case GameInfo.TYPES.ON_DEATH_ABILITY => Games.deathAbility(info.gameID, info.summonID(), info.changes())

          case GameInfo.TYPES.NEXT_STEP=>
            Games.nextStep(info.gameID,info.step)

          case GameInfo.TYPES.NEXT_TURN=>
            Games.nextTurn(info.gameID,info.player,info.changes)

          case GameInfo.TYPES.PLAYER_WON=>
            Games.playerWon(info.gameID, info.player)
        }
    }
  }
  object Duel{
    def setDefenses(gameID: Int, defenses: Array[(Integer, Integer)]){
      objOut.writeObject(GameAction.setDefenders(gameID, defenses))
    }

    def selectHand(gameID: Int, cardsToMull: Array[Int]){
      objOut.writeObject(GameAction.mulligan(gameID, cardsToMull))
    }

    def setAttackers(gameID: Int, attackerIDs: Array[Int]){
      objOut.writeObject(GameAction.setAttackers(gameID, attackerIDs))
    }

    def nextTurn(gameID: Int){
      objOut.writeObject(GameAction.endTurn(gameID))
    }

    def nextStep(gameID: Int){
      objOut.writeObject(GameAction.endStep(gameID))
    }

    def playCard(gameID: Int, graphicalCard: GraphicalRemoteCard){
      objOut.writeObject(GameAction.playCard(gameID, graphicalCard.remoteCard.id))
    }

    def queue(deck: Deck){
      var msgId: Int = 0
      responsesWaiting.synchronized{
        if(responsesWaiting.nonEmpty){
          msgId = responsesWaiting.last._1 + 1
        }
        responsesWaiting += (msgId, (msg: ResponseToClient)=>{msg.getType match{
          case ResponseToClient.TYPE.OK => Dialog.showMessage(null,"New game queue joined.")
          case ResponseToClient.TYPE.DENIED => Dialog.showMessage(null,msg.getData()(0))
        }
        })
      }
      objOut.writeObject(new RequestToServer(msgId, deck))
    }  
  }
  
  object Chat{
    def sendMessage(msg: String){
      objOut.writeObject(new ChatToServer(ChatToServer.TARGET.ROOM, "main", msg))
    }
  }

  private def requestJoinChat(name: String){
    var msgId: Int = 0
    responsesWaiting.synchronized{
      if(responsesWaiting.nonEmpty){
        msgId = responsesWaiting.last._1 + 1
      }
      responsesWaiting += (msgId, (msg: ResponseToClient)=>{LobbyTab.addChat(name, msg.getData)})
    }
    objOut.writeObject(new RequestToServer(msgId, RequestToServer.REQUEST.JOIN_CHAT,name))
  }

}