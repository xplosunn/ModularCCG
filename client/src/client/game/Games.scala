package client.game

import java.util.concurrent.ConcurrentHashMap
import javax.swing.JOptionPane

import client.ClientSession
import common.card.ability.change.GameChange
import common.game.{GameSteps, RemoteCard}
import client.window.tab.DuelTab

object Games{private val games = new ConcurrentHashMap[Int,DuelTab]()

  def handPreMulligan(id: Int, cards: Array[RemoteCard]){
    val game = games.get(id)
    if(game != null)
      game.handPreMulligan(cards)
  }

  def hand(id: Int, cardIDs: Array[Int]){
    val game = games.get(id)
    if(game != null)
      game.hand(cardIDs)
  }

  def nextStep(id: Int, step: GameSteps){
    val game = games.get(id)
      if(game != null)
        game.nextStep(step)
  }

  def nextTurn(id: Int, player: String, changes: Array[GameChange]){
    val game = games.get(id)
    if(game != null)
      game.nextTurn(player, changes)
  }

  def applyGameChanges(id: Int, changes: Array[GameChange]){
    val game = games.get(id)
    if(game != null)
      game.applyChanges(changes)
  }

  def attackersAnnounced(id: Int, attackerIDs: Array[Int]){
    val game = games.get(id)
    if(game != null)
      game.attackersAnnounced(attackerIDs)
  }

  def cardPlayed(id: Int, card: RemoteCard, changes: Array[GameChange]){
    val game = games.get(id)
    if(game != null)
      game.cardPlayed(card,changes)
  }


  def combatAbility(id: Int, summonID: Int, changes: Array[GameChange]){
    val game = games.get(id)
    if(game != null)
      game.combatAbility(summonID, changes)
  }

  def deathAbility(id: Int, summonID: Int, changes: Array[GameChange]){
    val game = games.get(id)
    if(game != null)
      game.deathAbility(summonID, changes)
  }

  def newDuel(panel: DuelTab){
    if(games.get(panel.gameID) == null)
      games.put(panel.gameID, panel)
  }

  def playerWon(id: Int, winner: String){
    val game = games.get(id)
    if(game != null){
      games.remove(game)
      JOptionPane.showMessageDialog(null, winner + " won the game")
      ClientSession.SessionVars.mainWindow.removeGameTab(game)
    }
  }
}