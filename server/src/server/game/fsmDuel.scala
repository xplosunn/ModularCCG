package server.game

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, FSM}
import com.google.common.base.Stopwatch
import common.card.Deck
import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import common.card.ability.change.{PlayerValueChange, SummonValueChange, CardDraw, GameChange}
import common.game.{GameSteps, RemoteCard}
import common.network.messages.serverToClient.GameInfo
import server.{game, ClientHandler}
import server.game.card.ability.{SpellAbilityEffectLibrary, SummonAbilityEffectLibrary}
import server.game.card.{GameSpell, BattlefieldSummon, GameSummon, GameCard}
import server.game.exception.{GameTiedException, PlayerWonException}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
 * Created by HugoSousa on 14-04-2015.
 */
// received events
final case class NewDuel(gameState: GameState)
final case class HandSelected(player: String, cardIDs: Array[Int])
final case class PlayCard(player: String, id: Int)
final case class NextStep(player: String, currentStep: GameSteps)
final case class EndTurn(player: String)
final case class SetAttackers(player: String, attackerIDs: Array[Int])
final case class SetDefenders(player: String, attackerIDs: Array[(Int, Int)])

//final case class SetTarget(ref: ActorRef)
//final case class Queue(obj: Any)
//case object Flush

// sent events

// states
sealed trait State
case object Available extends State
case object HandSelection extends State
case object Main_1 extends State
case object Combat_Attack extends State
case object Combat_Defend extends State
case object Main_2 extends State

sealed trait Data
case object NoData extends Data
case class StartingHandsSelected(_1: Boolean, _2: Boolean) extends Data


class fsmDuel extends FSM[State, Data] with Game{
  var summonsPlayedThisTurn = new ArrayBuffer[BattlefieldSummon]()

  val id = Duel.nextGameID

  var gameState: GameState = null

  private var _nextCardID = -1

  def nextCardID: Int = {
    synchronized{
      _nextCardID += 1
      _nextCardID
    }
  }

  startWith(Available, NoData)

  onTransition{
    case Available -> HandSelection =>
      gameState.players.foreach(p => {
        p.shuffleDeck()
        (1 to 6).foreach(_ => p.drawCard)
        p.handler.sendMessageToClient(GameInfo.handPreMulligan(id, p.hand.map(_.remoteCard).toArray))
      })
    case _ -> Main_1 =>
      gameState.players.foreach(_.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.MAIN_1st)))
      updateTimeout()
    case _ -> Main_2 =>
      gameState.players.foreach(_.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.MAIN_2nd)))
      updateTimeout()
    case _ -> Combat_Attack =>
      gameState.players.foreach(_.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.COMBAT_Attack)))
      updateTimeout()
    case _ -> Combat_Defend =>
      gameState.players.foreach(_.handler.sendMessageToClient(GameInfo.nextStep(id, GameSteps.COMBAT_Defend)))
      updateTimeout()
    case _ -> Available =>
      gameState = null
      cancelTimer("UNHANDLED_MESSAGE_TIMEOUT")
  }

  def updateTimeout() {
    setTimer("UNHANDLED_MESSAGE_TIMEOUT", StateTimeout, Duel.SECONDS_TO_ACT seconds, false)
  }

  //Available State
  when(Available) {
    case Event(NewDuel(gs), _) =>
      gameState = gs
      goto(HandSelection).using(StartingHandsSelected(false, false))
  }

  //Hand Selection State
  when(HandSelection, stateTimeout = Duel.SECONDS_TO_MULLIGAN seconds) {
    case Event(HandSelected(player, cardIDs), StartingHandsSelected(_1, _2)) =>
      var startingHandsSelected = (_1, _2)
      player match {
        case p1User if p1User == gameState.players(0).handler.userName =>
          startingHandsSelected = (true, startingHandsSelected._2)
          processPlayerHand(gameState.players(0), cardIDs)
        case p2User if p2User == gameState.players(1).handler.userName =>
          startingHandsSelected = (startingHandsSelected._1, true)
          processPlayerHand(gameState.players(1), cardIDs)
      }
      if(startingHandsSelected._1 && startingHandsSelected._2)
        endTurn
      else
        stay using StartingHandsSelected(startingHandsSelected._1, startingHandsSelected._2)

    case Event(StateTimeout, StartingHandsSelected(_1, _2)) =>
      if(!_1) processPlayerHand(gameState.players(0), null)
      if(!_2) processPlayerHand(gameState.players(1), null)
      goto(Main_1)
  }

  def processPlayerHand(player: Player, cardIDs: Array[Int]): Unit ={
    if(cardIDs != null && cardIDs.size == 3 &&
      cardIDs.forall(mullCardID => player.hand.exists(_.id == mullCardID))){
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
  when(Main_1){
    case Event(StateTimeout,_) =>
      if((gameState.activePlayer.battlefield -- summonsPlayedThisTurn).size > 0)
        goto(Combat_Attack)
      else
        goto(Main_2)

    case Event(NextStep(player, currentStep), _) =>
      if(player != gameState.activePlayer.handler.userName || currentStep != GameSteps.MAIN_1st)
        stay()
      else if((gameState.activePlayer.battlefield -- summonsPlayedThisTurn).size > 0)
        goto(Combat_Attack)
      else
        goto(Main_2)

    case Event(EndTurn(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      else endTurn

    case Event(PlayCard(player, cardID), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      else{
        updateTimeout()
        try{
          playCard(cardID)
          stay()
        }catch{
          case e: PlayerWonException =>
            playerWon(e)
            goto(Available)
          case e: GameTiedException =>
            gameTied(e)
            goto(Available)
        }
      }
  }

  //Main_2 State
  when(Main_2){
    case Event(StateTimeout,_) =>
      endTurn

    case Event(NextStep(player, currentStep), _) =>
      if(player != gameState.activePlayer.handler.userName || currentStep != GameSteps.MAIN_2nd) stay()
      else endTurn

    case Event(EndTurn(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      else endTurn

    case Event(PlayCard(player, cardID), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      else{
        try{
          updateTimeout()
          playCard(cardID)
          stay()
        }catch{
          case e: PlayerWonException =>
            playerWon(e)
            goto(Available)
          case e: GameTiedException =>
            gameTied(e)
            goto(Available)
        }
      }
  }

  def playCard(cardID: Int) = {
    val cardsFiltered = gameState.activePlayer.hand
      .filter(card => card.id == cardID && card.card.cost <= gameState.activePlayer.availableMana)
    if(cardsFiltered.isEmpty){
      println("card id ->" + cardID)
      println("ap" + gameState.activePlayer.hand.map(gc => gc.id))
    }else {
      val cardToPlay = cardsFiltered(0)
      gameState.activePlayer.availableMana -= cardToPlay.cost
      processPlayCard(cardToPlay)
    }
  }

  def endTurn = {
    gameState.nextTurn()
    summonsPlayedThisTurn.clear()
    val draw = gameState.activePlayer.drawCard
    gameState.activePlayer.handler.sendMessageToClient(GameInfo.nextTurn(id, gameState.activePlayer.handler.userName, Array(draw)))
    gameState.nonActivePlayer.handler.sendMessageToClient(GameInfo.nextTurn(id, gameState.activePlayer.handler.userName, Array(new CardDraw(draw.drawingPlayer, null, draw.deckEnded))))
    goto(Main_1)
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
        val battlefieldSummon = new BattlefieldSummon(gameCard)
        summonsPlayedThisTurn += battlefieldSummon
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
    try {
      gameState.checkPlayerState()
    }catch{
      case e: PlayerWonException =>
        playerWon(e)
        goto(Available)
      case e: GameTiedException =>
        gameTied(e)
        goto(Available)
    }
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
  when(Combat_Attack){
    case Event(SetAttackers(player, attackerIDs), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      else{
        gameState.setAttackers(gameState.activePlayer.battlefield.filter(s => attackerIDs.contains(s.id)))
        if(gameState.nonActivePlayer.battlefield.size > 0)
          goto(Combat_Defend)
        else{
          try{
            processBattle()
            goto(Main_2)
          }catch{
            case e: PlayerWonException =>
              playerWon(e)
              goto(Available)
            case e: GameTiedException =>
              gameTied(e)
              goto(Available)
          }
        }
      }

    case Event(NextStep(player, currentStep), _) =>
      if(player != gameState.activePlayer.handler.userName || currentStep != GameSteps.COMBAT_Attack) stay()
      else goto(Main_2)

    case Event(StateTimeout,_) =>
      goto(Main_2)

    case Event(EndTurn(player), _) =>
      if(player != gameState.activePlayer.handler.userName) stay()
      else endTurn
  }

  //Combat_Defend State
  when(Combat_Defend){
    case Event(SetDefenders(player, defenses), _) =>
      if(player == gameState.activePlayer.handler.userName || defenses == null) stay()
      else {
        def nonNullTuple(tuple: (BattlefieldSummon, BattlefieldSummon)) = tuple._1 != null && tuple._2 != null

        def findSummonInBattleField(owner: Player, id: Int): BattlefieldSummon =
          owner.battlefield.collectFirst({ case gc: BattlefieldSummon if gc.id == id => gc }).get

        val defenseSummons = defenses.collect({ case (attackerID, defenderID) =>
          (findSummonInBattleField(gameState.activePlayer, attackerID), findSummonInBattleField(gameState.nonActivePlayer, defenderID))
        })
          .filter(nonNullTuple)

        gameState.setDefenders(defenseSummons)

        val defenseIDs = gameState.defenses.collect({ case (gs, d) => Array[Int](gs.id) ++ d.map(_.id) }).toArray
        gameState.players.foreach(p => p.handler.sendMessageToClient(GameInfo.defenders(id, defenseIDs)))
        try {
          processBattle()
          goto(Main_2)
        } catch {
          case e: PlayerWonException =>
            playerWon(e)
            goto(Available)
          case e: GameTiedException =>
            gameTied(e)
            goto(Available)
        }
      }

    case Event(StateTimeout,_) =>
      try{
        processBattle()
        goto(Main_2)
      }catch{
        case e: PlayerWonException =>
          playerWon(e)
          goto(Available)
        case e: GameTiedException =>
          gameTied(e)
          goto(Available)
      }
  }

  private def processBattle() {
    if (gameState.attackers != null && gameState.attackers.size > 0){
      val phaseMessage = GameInfo.nextStep(id, GameSteps.COMBAT_Battle)
      gameState.players.foreach(_.handler.sendMessageToClient(phaseMessage))
      var battleChanges = new Array[GameChange](0)
      var playerLifeChanged = false
      gameState.defenses.foreach({case (attacker, defenders) =>
        if(defenders.size >= 1){
          defenders.foreach(defender => {
            attacker.changeLifeBy(0-defender.power)
            defender.changeLifeBy(0-attacker.power)
            battleChanges ++= Array(new SummonValueChange(defender.id, GameChange.Value.LIFE, defender.life))
          })
          battleChanges ++= Array(new SummonValueChange(attacker.id, GameChange.Value.LIFE, attacker.life))
        }else{
          gameState.nonActivePlayer.changeLifeBy(0-attacker.power)
          playerLifeChanged = true
        }
      })
      if(playerLifeChanged){
        val player = gameState.nonActivePlayer
        battleChanges ++= Array(new PlayerValueChange(player.handler.userName, GameChange.Value.LIFE, player.lifeTotal))
      }

      val battleResultsMessage = GameInfo.gameChanges(id, battleChanges)
      gameState.players.foreach(_.handler.sendMessageToClient(battleResultsMessage))

      gameState.defenses.foreach(d => (d._2 + d._1).foreach(summon => {
        (0 until summon.card.MAXIMUM_ABILITIES).toStream.takeWhile(i => summon.card.abilityLevel(i) != -1).foreach(i => {
          if(SummonAbilityLibrary.abilityList(summon.card.abilityLibraryIndex(i)).timing == SummonAbility.ON_COMBAT){
            val triggerChanges = SummonAbilityEffectLibrary.effects(summon.card.abilityLibraryIndex(i)).apply(summon.card.abilityLevel(i), gameState, summon)
            if(triggerChanges.size > 0){
              val changesArray = new Array[GameChange](0) ++ triggerChanges
              gameState.players.foreach(p => p.handler.sendMessageToClient(GameInfo.onCombatAbility(id, summon.id, changesArray)))
            }
          }
        })
      }))

      gameState.checkBattlefieldState()
      processDeathTriggers()
      gameState.checkPlayerState()
    }
  }

  private def playerWon(e: PlayerWonException){
    gameState.players.foreach(_.handler.sendMessageToClient(GameInfo.playerWon(id, e.player)))
  }

  private def gameTied(e: GameTiedException){
    gameState.players.foreach(_.handler.sendMessageToClient(GameInfo.gameTied(id)))
  }

  initialize()

  override def postStop(){}

}
