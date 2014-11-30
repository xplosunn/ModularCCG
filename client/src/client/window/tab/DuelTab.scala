package client.window.tab

import client.ClientSession.SessionVars
import client.window.tab.panel._
import common.card.ability.change._
import common.card.{Spell, Summon}
import common.game.{GameSteps, RemoteCard}

import scala.collection.mutable.ArrayBuffer
import scala.swing.BorderPanel.Position._
import scala.swing._

class DuelTab(val username: String, val gameID: Int, val players: Array[String]) extends BorderPanel{
  /*
  Game Logic
   */
  var activePlayerIndex = 0
  var currentStep: GameSteps = GameSteps.MAIN_1st
  var ownTurn = players(0) match { case `username` => true
                                   case _ => false }
  val idsOfSummonsPlayedThisTurn = new ArrayBuffer[Int]()
  var potentialDefender: GraphicalRemoteCard = null

  def cardPlayed(card: RemoteCard, changes: Array[GameChange]){
    var graphicalCard: GraphicalRemoteCard = null
    if(players(activePlayerIndex) == username){
      val handPanel = internalGamePanel.playerPanel(players(activePlayerIndex)).handPanel
      graphicalCard = handPanel.removeCard(card.id)
      handPanel.repaint()
    }else{
      val handPanel = internalGamePanel.playerPanel(players(activePlayerIndex)).handPanel
      handPanel.contents.remove(0)
      graphicalCard = new GraphicalRemoteCard(card,this)
      handPanel.repaint()
    }
    logTextArea.text += System.lineSeparator() + players(activePlayerIndex) + " played " + card.card.name + ". "
    val info = internalGamePanel.playerPanel(players(activePlayerIndex)).playerInfoPanel
    info.setManaLeft(info.getManaLeft - card.card.cost)
    card.card match {
      case summon: Summon =>
        idsOfSummonsPlayedThisTurn += graphicalCard.remoteCard.id
        val battleFieldPanel = internalGamePanel.playerPanel(players(activePlayerIndex)).battleFieldPanel
        battleFieldPanel.contents += new GraphicalRemoteCard(card,this)
        applyChanges(changes)
        battleFieldPanel.peer.getParent.repaint()
      case spell: Spell =>
        applyChanges(changes)
        info.pile += graphicalCard
        info.updatePile()
    }
  }

  def handPreMulligan(cards: Array[RemoteCard]){
    mulliganPanel.addCards(cards)
  }

  def hand(cardIDs: Array[Int]){
    mulliganPanel.clear()
    internalGamePanel.playerPanel(username).playerInfoPanel.setDeckSize(27)
    for(id<-cardIDs)
      internalGamePanel.playerPanel(username).handPanel.addCard(new GraphicalRemoteCard(mulliganPanel.cardLabels.filter(cl => cl.remoteCard.id == id)(0).remoteCard, this))
  }

  def cardClicked(card: GraphicalRemoteCard){
    if(card.remoteCard.owner == username)
      ownCardClicked(card)
    else
      opponentCardClicked(card)
  }

  def ownCardClicked(card: GraphicalRemoteCard) {
    if(ownTurn)
      currentStep match {
        case GameSteps.MAIN_1st | GameSteps.MAIN_2nd =>
          if(internalGamePanel.playerPanel(username).playerInfoPanel.getManaLeft >= card.remoteCard.card.cost
            && internalGamePanel.playerPanel(username).handPanel.contents.contains(card)) {
            val handler = SessionVars.serverHandler
            if (handler != null)
              handler.Duel.playCard(gameID, card)
          }
        case GameSteps.COMBAT_Attack =>
          if (!idsOfSummonsPlayedThisTurn.contains(card.remoteCard.id) && internalGamePanel.playerPanel(username).battleFieldPanel.contents.contains(card))
            card.attacking = !card.attacking
        case _ =>
      }
    else
      currentStep match {
        case GameSteps.COMBAT_Defend =>
          if(internalGamePanel.playerPanel(username).battleFieldPanel.contents.contains(card))
            potentialDefender = card
        case _ =>
      }
  }

  def opponentCardClicked(card: GraphicalRemoteCard){
    if(!ownTurn)
      currentStep match{
        case GameSteps.COMBAT_Defend =>
          if(card.attacking && potentialDefender != null){
            if(defenseLines.isDefender(potentialDefender))
              defenseLines.removeDefender(potentialDefender)
            else
              defenseLines.add(card, potentialDefender)
          }
        case _ =>
      }
  }

  def attackersAnnounced(attackerIDs: Array[Int]){
    val cards = internalGamePanel.playerPanel(players(activePlayerIndex)).battleFieldPanel.cards
    cards.foreach(c =>
      if(attackerIDs.contains(c.remoteCard.id)) {
        c.attacking = true
      }
    )
  }

  def combatAbility(summonID: Int, changes: Array[GameChange]){
    //TODO highlight summon by ID
    applyChanges(changes)
  }

  def deathAbility(summonID: Int, changes: Array[GameChange]){
    //TODO highlight summon by ID
    applyChanges(changes)
  }

  def nextStep(step: GameSteps){
    currentStep = step
    if(ownTurn){
      step match{
        case GameSteps.MAIN_2nd =>
          defenseLines.clear()
          internalGamePanel.playerPanel(players(activePlayerIndex)).battleFieldPanel.cards.foreach(
            card => card.attacking = false
          )
        case _ =>
      }
    }
    else {
      step match {
        case GameSteps.MAIN_2nd =>
          defenseLines.clear()
          internalGamePanel.playerPanel(players(activePlayerIndex)).battleFieldPanel.cards.foreach(
            card => card.attacking = false
          )
        case _ =>
      }
    }
    internalGamePanel.stepPanel.nextStep(step, ownTurn)
  }

  def nextTurn(player: String, changes: Array[GameChange]){
    activePlayerIndex = 1 - activePlayerIndex
    potentialDefender = null
    ownTurn = !ownTurn
    idsOfSummonsPlayedThisTurn.remove(0,idsOfSummonsPlayedThisTurn.size)
    internalGamePanel.stepPanel.nextTurn(ownTurn)
    internalGamePanel.stepPanel.nextStep(GameSteps.MAIN_1st, ownTurn)
    internalGamePanel.stepPanel.resetTurnOwner()
    logTextArea.text += System.lineSeparator() + "It is now " + player + "'s turn."
    applyChanges(changes)
    val playerInfo = internalGamePanel.playerPanel(player).playerInfoPanel
    playerInfo.setManaLeft(playerInfo.getManaTotal)
  }

  def applyChanges(changes: Array[GameChange]){
    var changesLog: String = ""
    for(c<-changes){
      c match{
        case change: CardDraw =>
          val infoPanel = internalGamePanel.playerPanel(change.deckOwner).playerInfoPanel
          infoPanel.setDeckSize(infoPanel.getDeckSize-1)
          val card = new GraphicalRemoteCard(change.card,this)
          internalGamePanel.playerPanel(change.drawingPlayer).handPanel.addCard(card)
          if(change.deckEnded)
            infoPanel.setLife(infoPanel.getLife-4)
          if(change.deckOwner == change.drawingPlayer)
            changesLog += change.drawingPlayer + " drew a card from his/her deck. "
          else
            changesLog += change.drawingPlayer + " drew a card from " + change.deckOwner + "'s deck. "

        case change: PlayerValueChange =>
          val panel = internalGamePanel.playerPanel(change.playerName)
          if (panel != null)
            change.changeValueIdentifier match {
              case GameChange.Value.LIFE =>
                panel.playerInfoPanel.setLife(change.effectiveNewValue)
              case GameChange.Value.MANA =>
                panel.playerInfoPanel.setManaTotal(change.effectiveNewValue)
            }

        case change: ZoneChange =>
          var found = false
          var graphicalCard: GraphicalRemoteCard = null
          players.takeWhile(_ => !found).foreach(
            player => {
              val playerPanel = internalGamePanel.playerPanel(player)
              playerPanel.battleFieldPanel.contents.takeWhile(_ => !found).foreach {
                case card: GraphicalRemoteCard =>
                  if (card.remoteCard.id == change.cardID) {
                    graphicalCard = card
                    playerPanel.battleFieldPanel.contents -= card
                    found = true
                  }
              }
              playerPanel.playerInfoPanel.pile.takeWhile(_ => !found).foreach {
                card => {
                  if (card.remoteCard.id == change.cardID) {
                    graphicalCard = card
                    playerPanel.playerInfoPanel.pile -= card
                    playerPanel.playerInfoPanel.updatePile()
                    found = true
                  }
                }
              }
            }
          )
          val playerPanel = internalGamePanel.playerPanel(change.targetZoneOwner)
          change.targetZone match {
            case GameChange.Zone.BATTLEFIELD =>
              playerPanel.battleFieldPanel.contents += graphicalCard
              playerPanel.battleFieldPanel.repaint()
            case GameChange.Zone.DECK =>
              playerPanel.playerInfoPanel.setDeckSize(playerPanel.playerInfoPanel.getDeckSize+1)
            case GameChange.Zone.HAND =>
              playerPanel.handPanel.addCard(graphicalCard)
            case GameChange.Zone.PILE =>
              playerPanel.playerInfoPanel.pile += graphicalCard
              playerPanel.playerInfoPanel.updatePile()
          }

        case change: SummonValueChange =>
          players.foreach(player=>{
            var deadSummons = new ArrayBuffer[GraphicalRemoteCard]()
            val playerPanel = internalGamePanel.playerPanel(player)
            playerPanel.battleFieldPanel.contents.foreach {
              case gr: GraphicalRemoteCard =>
                if (gr.remoteCard.id == change.summonID) {
                  change.change match {
                    case GameChange.Value.POWER => gr.currentPower = change.value
                    case GameChange.Value.LIFE =>
                      gr.currentLife = change.value
                      if (gr.currentLife <= 0) {
                        gr.restorePowerAndLife()
                        deadSummons += gr
                      }
                  }
                }
            }
            if(deadSummons.size > 0){
              playerPanel.battleFieldPanel.contents --= deadSummons
              playerPanel.playerInfoPanel.pile ++= deadSummons
              playerPanel.playerInfoPanel.updatePile()
            }
            playerPanel.battleFieldPanel.repaint()
          })
      }
    }
    logTextArea.text += System.lineSeparator() + changesLog
  }

  /*
  Game Panels
   */
  //Card Preview and Log Panel
  val cardViewer = new CardPreviewPanel(null)
  private val logTextArea = new TextArea()
  logTextArea.lineWrap = true
  logTextArea.wordWrap = true
  logTextArea.editable = false
  private val rightPanel = new BoxPanel(Orientation.Vertical){
    contents += cardViewer
    contents += new Label(" ")
    contents += new ScrollPane(logTextArea)
  }
  layout(rightPanel) = East

  //Panel holding everything else

  val internalGamePanel = new InternalGamePanel()
  val defenseLines = new DefenseLines()
  val mulliganPanel = new MulliganPanel(gameID,cardViewer)
  val layeredPanel = new LayeredPanel{
    add(internalGamePanel, 1)
    add(defenseLines, 2)
    add(mulliganPanel, 3)
  }
  layout(layeredPanel) = Center

  class InternalGamePanel extends BoxPanel(Orientation.Vertical){

    def playerPanel(player: String): PlayerPanel = {
      if(player == username)
        bottomPlayerPanel
      else
        topPlayerPanel
    }

    //Player Panels and Step Panel
    val stepPanel = new StepPanel(DuelTab.this)
    private val topPlayerPanel: PlayerPanel = players(0) match {
      case `username` => new PlayerPanel(players(1),PlayerPanel.Positions.Top, cardViewer, null)
      case _ => new PlayerPanel(players(0),PlayerPanel.Positions.Top, cardViewer, null)
    }
    private val bottomPlayerPanel: PlayerPanel = players(0) match {
      case `username` => new PlayerPanel(players(0),PlayerPanel.Positions.Bottom, cardViewer, stepPanel)
      case _ => new PlayerPanel(players(1),PlayerPanel.Positions.Bottom, cardViewer, stepPanel)
    }
    contents += topPlayerPanel.panel
    contents += bottomPlayerPanel.panel
  }
}