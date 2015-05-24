package server.game

import akka.actor.{Actor, ActorRef, FSM}
import common.card.Deck
import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import common.card.ability.change.{CardDraw, GameChange}
import common.game.{GameSteps, RemoteCard}
import common.network.messages.serverToClient.GameInfo
import server.ClientHandler
import server.game.card.ability.{SpellAbilityEffectLibrary, SummonAbilityEffectLibrary}
import server.game.card.{GameSpell, BattlefieldSummon, GameSummon, GameCard}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
 * Created by HugoSousa on 14-04-2015.
 */
// received events
final case class HandSelected(player: String, cardIDs: Array[Int])
final case class PlayCard(player: String, id: Int)
final case class NextStep(player: String)
final case class EndTurn(player: String)

//final case class SetTarget(ref: ActorRef)
//final case class Queue(obj: Any)
//case object Flush

// sent events

// states
sealed trait State
case object HandSelection extends State
case object Main_1 extends State
case object Combat_Attack extends State
case object Combat_Defend extends State
case object Combat_Battle extends State
case object Main_2 extends State

sealed trait Data
case object StateStarted extends Data
case object NoData extends Data


class fsmDuel(playerOneHandler: ClientHandler, playerTwoHandler: ClientHandler, playerOneDeck: Deck, playerTwoDeck: Deck)
  extends FSM[State, Data] with Actor with Game{

  val p1 = new Player(playerOneDeck, playerOneHandler, this)
  val p2 = new Player(playerTwoDeck, playerTwoHandler, this)
  val id = Duel.nextGameID
  val gameState = new GameState(Array(p1,p2), this)

  var startingHandsSelected: (Boolean, Boolean) = (false, false)
  var secondsLeftThisTurn = Duel.SECONDS_PER_TURN
  var summonsPlayedThisTurn = new ArrayBuffer[GameSummon]()

  private var _nextCardID = -1

  def nextCardID: Int = {
    synchronized{
      _nextCardID += 1
      _nextCardID
    }
  }

  startWith(HandSelection, StateStarted)

  //Hand Selection State
  when(HandSelection, stateTimeout = Duel.SECONDS_TO_MULLIGAN seconds) {
    case Event(StateStarted, _) =>
      p1.handler.sendMessageToClient(GameInfo.handPreMulligan(id, p1.hand.map(_.remoteCard).toArray))
      p2.handler.sendMessageToClient(GameInfo.handPreMulligan(id, p2.hand.map(_.remoteCard).toArray))
      stay using NoData

    case Event(NextStep(player), _) =>
      if (player != gameState.activePlayer.handler.userName) stay()
      goto(Combat_Attack).using(StateStarted)

    case Event(HandSelected(player, cardIDs), _) =>
      player match {
        case p1User if p1User == p1.handler.userName =>
          startingHandsSelected = (true, startingHandsSelected._2)
          processPlayerHand(p1, cardIDs)
        case p2User if p2User == p2.handler.userName =>
          startingHandsSelected = (startingHandsSelected._1, true)
          processPlayerHand(p2, cardIDs)
      }
      if(startingHandsSelected._1 && startingHandsSelected._2)
        goto(Main_1)
      stay using NoData

    case Event(StateTimeout, _) =>
      if(!startingHandsSelected._1) processPlayerHand(p1, null)
      if(!startingHandsSelected._2) processPlayerHand(p2, null)
      goto(Main_1).using(StateStarted)
  }

  def processPlayerHand(player: Player, cardIDs: Array[Int]): Unit ={
    if(cardIDs != null && cardIDs.size == 3 &&
      cardIDs.forall(mullCardID => player.hand.exists(gc => gc.id == mullCardID))){
      for(id <- cardIDs){
        val cardBackToDeck = player.hand.filter(gc => gc.id == id)(0)
        player.hand -= cardBackToDeck
        player.deck.cards += cardBackToDeck
      }
    }
    else{
      for(_ <- 0 until 3)
        player.deck.cards += player.hand.remove(0)
    }
    player.shuffleDeck()
    val handIDs = new Array[Int](3)
    for(j<- handIDs.indices)
      handIDs(j) = player.hand(j).id
    player.handler.sendMessageToClient(GameInfo.hand(id, handIDs))
  }

  //Main_1 State
  when(Main_1, stateTimeout = Duel.SECONDS_PER_TURN seconds){
    case Event(StateStarted,_) =>
      p1.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.MAIN_1st))
      p2.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.MAIN_1st))
      stay()

    case Event(StateTimeout,_) =>
      endTurn

    case Event(NextStep(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      goto(Combat_Attack).using(StateStarted)

    case Event(EndTurn(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      endTurn

    case Event(PlayCard(player, cardID), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      playCard(cardID)
  }

  //Main_2 State
  when(Main_2, stateTimeout = secondsLeftThisTurn seconds){
    case Event(StateStarted,_) =>
      p1.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.MAIN_2nd))
      p2.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.MAIN_2nd))
      stay()

    case Event(StateTimeout,_) =>
      endTurn

    case Event(NextStep(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      endTurn

    case Event(EndTurn(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      endTurn

    case Event(PlayCard(player, cardID), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      playCard(cardID)
  }

  def playCard(cardID: Int) = {
    val cardsFiltered = gameState.activePlayer.hand
      .filter(card => card.id == cardID && card.card.cost <= gameState.activePlayer.availableMana)
    if(cardsFiltered.isEmpty) stay()
    val cardToPlay = cardsFiltered(0)
    gameState.activePlayer.availableMana -= cardToPlay.cost
    processPlayCard(cardToPlay)
    stay()
  }

  def endTurn = {
    gameState.nextTurn()
    summonsPlayedThisTurn.clear()
    secondsLeftThisTurn = Duel.SECONDS_PER_TURN
    goto(Main_1).using(StateStarted)
  }

  /**
   * Processes the actual playing of the card, including zone change and changes by abilities.
   * To be called after confirmation that the specified card is in the active player's hand.
   * @param gameCard the card
   */
  private def processPlayCard(gameCard: GameCard){
    val activePlayer = gameState.activePlayer
    activePlayer.hand -= gameCard
    val changes = new ArrayBuffer[GameChange]()
    gameCard match {
      case gameCard: GameSummon =>
        summonsPlayedThisTurn += gameCard
        val battlefieldSummon = new BattlefieldSummon(gameCard)
        activePlayer.battlefield += battlefieldSummon
        (0 until gameCard.card.MAXIMUM_ABILITIES).toStream.takeWhile(i => gameCard.card.abilityLibraryIndex(i) != -1).foreach(
          i => {val index = gameCard.card.abilityLibraryIndex(i)
            if (SummonAbilityLibrary.abilityList(index).timing == SummonAbility.ON_SUMMON){
              changes ++= SummonAbilityEffectLibrary.effects(index).apply(gameCard.card.abilityLevel(i),gameState, battlefieldSummon)
              if(SummonAbilityEffectLibrary.effects(index).changesBattleFieldState)
                gameState.checkBattlefieldState()
            }
          })
      case gameCard: GameSpell =>
        (0 until gameCard.card.MAXIMUM_ABILITIES).toStream.takeWhile(i => gameCard.card.abilityLibraryIndex(i) != -1).foreach(
          i => {val index = gameCard.card.abilityLibraryIndex(i)
            changes ++= SpellAbilityEffectLibrary.effects(index).apply(gameCard.card.abilityLevel(i),gameState)
            if(SpellAbilityEffectLibrary.effects(index).changesBattleFieldState)
              gameState.checkBattlefieldState()
          })
        activePlayer.pile += gameCard
    }
    val messageToAP = GameInfo.cardPlayed(id, gameCard.remoteCard, changes.toArray)
    gameState.activePlayer.handler.sendMessageToClient(messageToAP)

    val changesToNAP = new Array[GameChange](changes.size)
    for (i <- changes.indices) {
      changes(i) match {
        case change: CardDraw =>
          changesToNAP(i) = new CardDraw(change.drawingPlayer, null, change.deckEnded)
        case any => changesToNAP(i) = any
      }
    }
    val messageToNAP = GameInfo.cardPlayed(id, gameCard.remoteCard, changesToNAP)
    gameState.nonActivePlayer.handler.sendMessageToClient(messageToNAP)

    processDeathTriggers()
    gameState.checkPlayerState()
  }

  private def processDeathTriggers() {
    var nextDeathTrigger: (BattlefieldSummon, Int, Int) = gameState.nextDeathTrigger
    while(nextDeathTrigger != null){
      val changeBuffer = SummonAbilityEffectLibrary.effects(nextDeathTrigger._2).apply(nextDeathTrigger._3, gameState, nextDeathTrigger._1)
      val changeArray = new Array[GameChange](0) ++ changeBuffer
      gameState.checkBattlefieldState()
      val message = GameInfo.onDeathAbility(id, nextDeathTrigger._1.id, changeArray)
      gameState.players.foreach(player => player.handler.sendMessageToClient(message))
      nextDeathTrigger = gameState.nextDeathTrigger
    }
  }

  //Combat_Attack State
  when(Combat_Attack, stateTimeout = Duel.SECONDS_PER_TURN seconds){
    case Event(StateStarted,_) =>
      p1.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.COMBAT_Attack))
      p2.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.COMBAT_Attack))
      stay()


  }

  initialize

  override def postStop(){}
}
