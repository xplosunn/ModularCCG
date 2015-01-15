package server.game

import java.util.concurrent.TimeUnit

import com.google.common.base.Stopwatch
import common.card.Deck
import common.card.ability.change.{CardDraw, GameChange, PlayerValueChange, SummonValueChange}
import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import common.game.{GameSteps, RemoteCard}
import common.network.messages.serverToClient.GameInfo
import server.ClientHandler
import server.game.card.{GameSpell, GameCard, GameSummon}
import server.game.card.ability.{SpellAbilityEffectLibrary, SummonAbilityEffectLibrary}
import server.game.exception.{GameTiedException, PlayerWonException}

import scala.collection.mutable.ArrayBuffer

object Duel {
  private var gameID = -1
  val SECONDS_TO_MULLIGAN = 30
  //TODO: maybe change the time per turn to time per mainphase (?)
  val SECONDS_PER_TURN = 90
  val SECONDS_TO_CHOOSE_DEFENDERS = 30
  val EXTRA_SECONDS = 30

  def nextGameID = {
    synchronized{
      gameID += 1
      gameID
    }
  }
}

class Duel(playerOneHandler: ClientHandler, playerTwoHandler: ClientHandler, playerOneDeck: Deck, playerTwoDeck: Deck) extends Thread with Game{
  val p1 = new Player(playerOneDeck, playerOneHandler, this)
  val p2 = new Player(playerTwoDeck, playerTwoHandler, this)
  val id = Duel.nextGameID
  protected val gameState = new GameState(Array(p1,p2), this)
  private val mulligans = new Array[Array[Int]](2)

  private var _nextCardID = -1

  def nextCardID: Int = {
    synchronized{
      _nextCardID += 1
      _nextCardID
    }
  }

  protected object CurrentTurn {
    var secondsLeft = Duel.SECONDS_PER_TURN
    var currentStep: GameSteps = GameSteps.HAND_SELECTION

    var defenderSecondsLeft = Duel.SECONDS_TO_CHOOSE_DEFENDERS
    var cardsToPlay = new ArrayBuffer[GameCard]()
    var summonsPlayed = new ArrayBuffer[GameSummon]()
    var ongoingPhase = false
    var ended = false

    def reset(){
      currentStep = GameSteps.MAIN_1st
      cardsToPlay = new ArrayBuffer[GameCard]()
      summonsPlayed = new ArrayBuffer[GameSummon]()
      secondsLeft = Duel.SECONDS_PER_TURN
      defenderSecondsLeft = Duel.SECONDS_TO_CHOOSE_DEFENDERS
      ongoingPhase = false
      ended = false
    }
  }

  def mulligan(player: String, cardIDs: Array[Int]){
    synchronized {
      if(CurrentTurn.currentStep == GameSteps.HAND_SELECTION) {
        if (gameState.players(0).handler.getUserName == player)
          mulligans(0) = cardIDs
        else
          mulligans(1) = cardIDs
        if (mulligans(0) != null && mulligans(1) != null)
          interrupt()
      }
    }
  }

  private def mulligansDone =
    mulligans(0) != null && mulligans(1) != null

  def isActivePlayer(s: String): Boolean =
    gameState.activePlayer.handler.getUserName == s

  def nextStep(){
    if(CurrentTurn.currentStep == GameSteps.MAIN_1st)
      synchronized {
        CurrentTurn.ongoingPhase = false
        interrupt()
      }
  }

  def endTurn(){
    if(CurrentTurn.currentStep == GameSteps.MAIN_1st || CurrentTurn.currentStep == GameSteps.MAIN_2nd)
      synchronized {
        CurrentTurn.ongoingPhase = false
        CurrentTurn.ended = true
        interrupt()
      }
  }

  override def run(){
    setupGame()
    try {
      while (true) {
        prepareTurn()
        if (!CurrentTurn.ended)
          mainPhase(first = true)
        if (!CurrentTurn.ended) {
          attack()
          defend()
          battle()
        }
        if (!CurrentTurn.ended)
          mainPhase(first = false)
      }
    } catch {
      case e: PlayerWonException => reportWin(e)
      case e: GameTiedException => //TODO report tie
      case e => e.printStackTrace() //TODO report error
    }
  }

  def addCardToBePlayed(cardID: Int){
    for(card<-gameState.activePlayer.hand.cards)
      if(card.id == cardID && card.card.cost <= gameState.activePlayer.availableMana){
        gameState.activePlayer.availableMana -= card.card.cost
        CurrentTurn.synchronized{
          CurrentTurn.cardsToPlay += card
        }
        synchronized{interrupt()}
        return
      }
  }

  def setAttackers(ids: Array[Int]){
    if (CurrentTurn.currentStep == GameSteps.COMBAT_Attack && ids != null) {
      val attackers = new ArrayBuffer[GameSummon]()
      gameState.activePlayer.battlefield.summons.foreach(
        s => if (ids.contains(s.id) && !CurrentTurn.summonsPlayed.contains(s)) {
          attackers += s
        })
      synchronized {
        gameState.setAttackers(attackers)
        interrupt()
      }
    }
  }

  def setDefenses(defenseIDs: Array[(Int, Int)]){
    if (CurrentTurn.currentStep == GameSteps.COMBAT_Defend && gameState.attackerCount > 0 && defenseIDs != null){
      def nonNullTuple(tuple: (GameSummon, GameSummon)) = tuple._1 != null && tuple._2 != null

      val defenses = defenseIDs.collect({case (attackerID, defenderID) =>
        (findSummonInBattleField(gameState.activePlayer, attackerID), findSummonInBattleField(gameState.nonActivePlayer, defenderID))})
        .filter(nonNullTuple)

      synchronized {
        gameState.setDefenders(defenses)
        interrupt()
      }
    }
  }

  private def findSummonInBattleField(owner: Player, id: Int): GameSummon = {
    owner.battlefield.cards.collectFirst({case gc: GameSummon if (gc.id == id) => gc}).get
  }

  /**
   * Processes the actual playing of the card, including zone change and changes by abilities.
   * To be called after confirmation that the specified card is in the active player's hand.
   * @param gameCard the card
   */
  private def processPlayCard(gameCard: GameCard){
    val activePlayer = gameState.activePlayer
    activePlayer.hand.cards -= gameCard
    var changes = new Array[GameChange](0)
    gameCard match {
      case gameCard: GameSummon =>
        CurrentTurn.summonsPlayed += gameCard
        activePlayer.battlefield.cards += gameCard
        (0 until gameCard.card.MAXIMUM_ABILITIES).toStream.takeWhile(i => gameCard.card.abilityLibraryIndex(i) != -1).foreach(
          i => {val index = gameCard.card.abilityLibraryIndex(i)
            if (SummonAbilityLibrary.abilityList(index).timing == SummonAbility.ON_SUMMON){
              changes ++= SummonAbilityEffectLibrary.effects(index).apply(gameCard.card.abilityLevel(i),gameState, gameCard)
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
        activePlayer.pile.cards += gameCard
    }
    val messageToAP = GameInfo.cardPlayed(id, new RemoteCard(gameCard.id, gameCard.owner.handler.getUserName, gameCard.card), changes)
    gameState.activePlayer.handler.sendMessageToClient(messageToAP)

    val changesToNAP = new Array[GameChange](changes.size)
    for (i <- changes.indices) {
      changes(i) match {
        case change: CardDraw =>
          changesToNAP(i) = new CardDraw(change.drawingPlayer, null, change.deckEnded)
        case any => changesToNAP(i) = any
      }
    }
    val messageToNAP = GameInfo.cardPlayed(id, new RemoteCard(gameCard.id, gameCard.owner.handler.getUserName, gameCard.card), changesToNAP)
    gameState.nonActivePlayer.handler.sendMessageToClient(messageToNAP)

    processDeathTriggers()
    gameState.checkPlayerState()
  }

  private def setupGame(){
    val activePlayerIndex = gameState.activePlayerIndex
    val players = gameState.players
    val names = players.collect({case p => p.handler.getUserName}).toArray
    players.foreach(p => {
      p.handler.sendMessageToClient(GameInfo.gameStarted(id, names))

      (0 until 6).foreach(_=> p.drawCard)

      val cards = p.hand.cards.collect({case gc => new RemoteCard(gc.id, gc.owner.handler.getUserName, gc.card)}).toArray
      p.handler.sendMessageToClient(GameInfo.handPreMulligan(id, cards))

      val changes: Array[GameChange] = players.filter(op => op != p).take(3).collect({case op => new CardDraw(op.handler.getUserName, null, false)}).toArray
      p.handler.sendMessageToClient(GameInfo.gameChanges(id, changes))
    })

    //Mulligans
    var mulliganing = true
    val stopWatch = Stopwatch.createStarted()
    synchronized{
      while (mulliganing) {
        val timeLeft = Duel.SECONDS_TO_MULLIGAN - stopWatch.elapsed(TimeUnit.SECONDS)
        if (!mulligansDone && timeLeft > 0 )
          try {
            wait(timeLeft)
          } catch {
            case e: InterruptedException =>
          }
        else{
          processMulligans()
          mulliganing = false
        }
      }
    }
  }

  private def processMulligans() {
    for(i<- 0 until 2){
      val player = gameState.players(i)
      //Validate mull
      if(mulligans(i) != null && mulligans(i).size == 3 &&
        mulligans(i).forall(mullCardID => player.hand.cards.exists(gc => gc.id == mullCardID))){
        for(id <- mulligans(i)){
          val cardBackToDeck = player.hand.cards.filter(gc => gc.id == id)(0)
          player.hand.cards -= cardBackToDeck
          player.deck.cards += cardBackToDeck
        }
      }
      else{
        for(_ <- 0 until 3)
          player.deck.cards += gameState.players(i).hand.cards.remove(0)
      }
      player.shuffleDeck()
      val handIDs = new Array[Int](3)
      for(j<- handIDs.indices)
        handIDs(j) = player.hand.cards(j).id
      player.handler.sendMessageToClient(GameInfo.hand(id, handIDs))
    }
  }

  private def prepareTurn(){
    gameState.nextTurn()
    CurrentTurn.reset()

    val cardDrawnChange = gameState.activePlayer.drawCard

    val activePlayerName = gameState.activePlayer.handler.getUserName
    val playerChange = new PlayerValueChange(activePlayerName, GameChange.Value.MANA, gameState.activePlayer.manaTotal)

    val messageToNAP = GameInfo.nextTurn(id,activePlayerName,
      Array[GameChange](
        new CardDraw(activePlayerName,null, cardDrawnChange.deckEnded),
        playerChange
      ))
    val messageToAP = GameInfo.nextTurn(id,activePlayerName,
      Array[GameChange](
        cardDrawnChange,
        playerChange
      ))

    if(p1 == gameState.activePlayer){
      p1.handler.sendMessageToClient(messageToAP)
      p2.handler.sendMessageToClient(messageToNAP)
    }else{
      p1.handler.sendMessageToClient(messageToNAP)
      p2.handler.sendMessageToClient(messageToAP)
    }

    gameState.checkPlayerState()
  }

  private def mainPhase(first: Boolean) {
    val phaseMessage = if(first)
      GameInfo.nextStep(id, GameSteps.MAIN_1st)
    else{
      CurrentTurn.currentStep = GameSteps.MAIN_2nd
      GameInfo.nextStep(id, GameSteps.MAIN_2nd)
    }
    p1.handler.sendMessageToClient(phaseMessage)
    p2.handler.sendMessageToClient(phaseMessage)
    CurrentTurn.ongoingPhase = true
    CurrentTurn.cardsToPlay.remove(0,CurrentTurn.cardsToPlay.size)
    val stopWatch = Stopwatch.createStarted()
    synchronized{
      while (CurrentTurn.ongoingPhase) {
        if (CurrentTurn.cardsToPlay.size > 0)
          processPlayCard(CurrentTurn.synchronized {
            CurrentTurn.cardsToPlay.remove(0)
          })
        else if (CurrentTurn.secondsLeft > 0)
          try {
            wait(CurrentTurn.secondsLeft * 1000)
            CurrentTurn.ongoingPhase = false
            //TODO implement extra time and maybe implement timer alert for player (?)
          } catch {
            case e: InterruptedException =>
          }
        else
          CurrentTurn.ongoingPhase = false
      }
    }
    val secondsPassed = stopWatch.elapsed(TimeUnit.SECONDS)
    if(secondsPassed > CurrentTurn.secondsLeft)
      CurrentTurn.secondsLeft = 0
    else
      CurrentTurn.secondsLeft -= secondsPassed.asInstanceOf[Int]
  }

  private def attack() {
    if (gameState.activePlayer.battlefield.summons.--(CurrentTurn.summonsPlayed).size > 0) {
      CurrentTurn.currentStep = GameSteps.COMBAT_Attack
      val phaseMessage = GameInfo.nextStep(id, GameSteps.COMBAT_Attack)
      p1.handler.sendMessageToClient(phaseMessage)
      p2.handler.sendMessageToClient(phaseMessage)
      CurrentTurn.ongoingPhase = true
      val stopWatch = Stopwatch.createStarted()
      synchronized {
        while (CurrentTurn.ongoingPhase) {
          if (gameState.attackersSet) {
            val attackerIDs = gameState.attackers.collect({case a => a.id}).toArray
            gameState.players.foreach(p => p.handler.sendMessageToClient(GameInfo.attackers(id, attackerIDs)))
            CurrentTurn.ongoingPhase = false
          }
          else if (CurrentTurn.secondsLeft > 0)
            try {
              wait(CurrentTurn.secondsLeft * 1000)
              CurrentTurn.ongoingPhase = false
              //TODO implement extra time and maybe implement timer alert for player (?)
            } catch {
              case e: InterruptedException =>
            }
          else
            CurrentTurn.ongoingPhase = false
        }
        val secondsPassed = stopWatch.elapsed(TimeUnit.SECONDS).asInstanceOf[Int]
        if (secondsPassed > CurrentTurn.secondsLeft)
          CurrentTurn.secondsLeft = 0
        else
          CurrentTurn.secondsLeft -= secondsPassed
      }
    }
  }

  private def defend() {
    if (gameState.attackerCount > 0 && gameState.nonActivePlayer.battlefield.summons.size > 0) {
      CurrentTurn.currentStep = GameSteps.COMBAT_Defend
      val phaseMessage = GameInfo.nextStep(id, GameSteps.COMBAT_Defend)
      p1.handler.sendMessageToClient(phaseMessage)
      p2.handler.sendMessageToClient(phaseMessage)
      var done = false
      synchronized {
        while (!done) {
          if (gameState.defendersSet) {
            val defenseIDs = gameState.defenses.collect({case (gs, d) => Array[Int](gs.id) ++ d.collect({case g => g.id})}).toArray
            gameState.players.foreach(p => p.handler.sendMessageToClient(GameInfo.defenders(id, defenseIDs)))
            done = true
          }
          else if (CurrentTurn.defenderSecondsLeft > 0)
            try {
              wait(CurrentTurn.defenderSecondsLeft * 1000)
              done = true
              //TODO implement extra time and maybe implement timer alert for player (?)
            } catch {
              case e: InterruptedException =>
            }
          else
            done = true
        }
      }
    }
  }

  private def battle() {
    if (gameState.attackers != null && gameState.attackers.size > 0){
      CurrentTurn.currentStep = GameSteps.COMBAT_Battle
      val phaseMessage = GameInfo.nextStep(id, GameSteps.COMBAT_Battle)
      p1.handler.sendMessageToClient(phaseMessage)
      p2.handler.sendMessageToClient(phaseMessage)
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
        battleChanges ++= Array(new PlayerValueChange(player.handler.getUserName, GameChange.Value.LIFE, player.lifeTotal))
      }

      val battleResultsMessage = GameInfo.gameChanges(id, battleChanges)
      p1.handler.sendMessageToClient(battleResultsMessage)
      p2.handler.sendMessageToClient(battleResultsMessage)

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

  private def processDeathTriggers() {
    var nextDeathTrigger: (GameSummon, Int, Int) = gameState.nextDeathTrigger
    while(nextDeathTrigger != null){
      val changeBuffer = SummonAbilityEffectLibrary.effects(nextDeathTrigger._2).apply(nextDeathTrigger._3, gameState, nextDeathTrigger._1)
      val changeArray = new Array[GameChange](0) ++ changeBuffer
      gameState.checkBattlefieldState()
      val message = GameInfo.onDeathAbility(id, nextDeathTrigger._1.id, changeArray)
      gameState.players.foreach(player => player.handler.sendMessageToClient(message))
      nextDeathTrigger = gameState.nextDeathTrigger
    }
  }

  private def reportWin(e: PlayerWonException){
    val message = GameInfo.playerWon(id,e.player)
    p1.handler.sendMessageToClient(message)
    p2.handler.sendMessageToClient(message)
  }
}