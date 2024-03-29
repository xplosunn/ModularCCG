package server.services

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Consumer, BiConsumer}

import common.card.Deck
import common.game.RemoteCard
import common.network.messages.clientToServer.{GameAction, RequestToServer}
import common.network.messages.serverToClient.ResponseToClient
import server.ClientHandler
import server.game.{Duel, Player}

import scala.collection.mutable.ArrayBuffer

object Games {
  private val duels = new ConcurrentHashMap[Int,Duel]()
  private var queuedForDuel: (ClientHandler,Deck) = null

  def duelCount: Int = duels.size()

  def duelInfo: String = {
    var res = ""
    duels.values().forEach(new Consumer[Duel] {
      override def accept(d: Duel){
        res += d.p1.handler.userName + " vs " + d.p2.handler.userName + System.lineSeparator
      }
    })
    res
  }

  def newDuel(playerOneHandler: ClientHandler, playerTwoHandler: ClientHandler, playerOneDeck: Deck, playerTwoDeck: Deck){
    val game = new Duel(playerOneHandler, playerTwoHandler, playerOneDeck, playerTwoDeck)
    duels.put(game.id, game)
    game.start()
  }

  def gameAction(action: GameAction, player: String){
    val game = duels.get(action.getGameID)
    if(game != null)
      if (game.isActivePlayer(player))
        action.getAction match {
          case GameAction.ACTIONS.MULLIGAN => game.mulligan(player, action.getCardIDs)
          case GameAction.ACTIONS.NEXT_STEP => game.nextStep()
          case GameAction.ACTIONS.END_TURN => game.endTurn()
          case GameAction.ACTIONS.PLAY_CARD => game.addCardToBePlayed(action.getCardID)
          case GameAction.ACTIONS.SET_ATTACKERS => game.setAttackers(action.getAttackerIDs)
          case _ =>
        }
      else
        action.getAction match {
          case GameAction.ACTIONS.MULLIGAN => game.mulligan(player, action.getCardIDs)
          case GameAction.ACTIONS.SET_DEFENDERS => game.setDefenses(action.getDefenses)
          case _ =>
        }
  }

  def queueForDuel(handler: ClientHandler, msg: RequestToServer){
    val deck: Deck = msg.getDeck
    if(deck != null && deck.validate)
      this.synchronized {
        if (queuedForDuel == null) {
          queuedForDuel = (handler, deck)
          handler.sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.OK, msg.getRequestID, null))
        }
        else if (queuedForDuel._1 == handler) {
          handler.sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.DENIED, msg.getRequestID, Array("Already queued.")))
        }
        else if(!queuedForDuel._1.isAlive){
          queuedForDuel = (handler, deck)
          handler.sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.OK, msg.getRequestID, null))
        }
        else {
          val previouslyQueued = queuedForDuel
          queuedForDuel = null
          handler.sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.OK, msg.getRequestID, null))
          newDuel(handler, previouslyQueued._1, deck, previouslyQueued._2)
        }
      }
    else
      handler.sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.DENIED, msg.getRequestID, Array("Invalid deck.")))
  }

  def playCard(gameID: Int, card: RemoteCard){
    val game = duels.get(gameID)
    if(game!=null){
      game.addCardToBePlayed(card.id)
      game.synchronized{game.notify()}
    }
  }

  def disconnect(handler: ClientHandler){
    val keysToRemove = new ArrayBuffer[Int]()
    duels.keySet().forEach(new Consumer[Int] {
      override def accept(key: Int) {
        val duel = duels.get(key)
        if(duel.p1.handler == handler || duel.p2.handler == handler){
          duel.playerDisconnected()
          keysToRemove += key
        }
      }
    })
    for(key<-keysToRemove)
      duels.remove(key)
  }
}