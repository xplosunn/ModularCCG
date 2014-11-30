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
  protected val gameState = new GameState(Array(p1,p2))
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

  def setDefenses(defenseIDs: Array[(Integer, Integer)]){
    if (CurrentTurn.currentStep == GameSteps.COMBAT_Defend && gameState.attackerCount > 0 && defenseIDs != null){
      val defenses = new ArrayBuffer[ArrayBuffer[GameSummon]]()
      defenseIDs.foreach(defenseTuple => {
        var alreadyInDefensesArray = false
        defenses.takeWhile(_ => !alreadyInDefensesArray).foreach(
          d =>
            if (d(0).id == defenseTuple._1) {
              alreadyInDefensesArray = true
              val defender = findSummonInBattleField(gameState.nonActivePlayer, defenseTuple._2)
              if (defender != null)
                d += defender
            })
        if(!alreadyInDefensesArray){
          val attacker = findSummonInBattleField(gameState.activePlayer, defenseTuple._1)
          val defender = findSummonInBattleField(gameState.nonActivePlayer, defenseTuple._2)
          if(attacker != null && defender != null){
            defenses += ArrayBuffer(attacker,defender)
          }
        }
      })
      synchronized {
        gameState.setDefenders(defenses)
        interrupt()
      }
    }
  }

  def findSummonInBattleField(owner: Player, id: Int): GameSummon = {
    owner.battlefield.cards.foreach{
      case summon: GameSummon =>
        if(summon.id == id)
          return summon

    }
    null
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
          changesToNAP(i) = new CardDraw(change.deckOwner, change.drawingPlayer, null, change.deckEnded)
        case any => changesToNAP(i) = any
      }
    }
    val messageToNAP = GameInfo.cardPlayed(id, new RemoteCard(gameCard.id, gameCard.owner.handler.getUserName, gameCard.card), changesToNAP)
    gameState.nonActivePlayer.handler.sendMessageToClient(messageToNAP)

    processDeathTriggers()
    gameState.checkPlayerState()
  }

  def setupGame(){
    val activePlayerIndex = gameState.activePlayerIndex
    val players = gameState.players
    val names = new Array[String](players.size)
    for(i<-players.indices){
      names(i) = players((activePlayerIndex+i)%players.size).handler.getUserName
    }
    for(p<-players){
      p.handler.sendMessageToClient(GameInfo.gameStarted(id, names))

      for(_<-0 until 6)
        p.drawCard
      val cards = new Array[RemoteCard](p.hand.cards.size)
      for(i<-cards.indices){
        val gameCard = p.hand.cards(i)
        cards(i) = new RemoteCard(gameCard.id, gameCard.owner.handler.getUserName, gameCard.card)
      }

      p.handler.sendMessageToClient(GameInfo.handPreMulligan(id, cards))

       //code before mulls
      val changes = new ArrayBuffer[GameChange]()
      /*for(i<-0 until 3){
        val localCard = p.drawCard
        val remoteCard = new RemoteGameCard(localCard.id,localCard.owner.handler.getUserName,localCard.card)
        changes += new CardDraw(p.handler.getUserName,p.handler.getUserName,remoteCard)
      }*/

      for(op<-players){
        if(op!=p){
          for(_<- 0 until 3)
            changes += new CardDraw(op.handler.getUserName,op.handler.getUserName, null, false)
        }
      }
      val changeArray = new Array[GameChange](changes.size)
      for(i<-changes.indices)
        changeArray(i) = changes(i)
      p.handler.sendMessageToClient(GameInfo.gameChanges(id, changeArray))
    }

    //Mulligans
    var mulliganing = true
    val stopWatch = Stopwatch.createStarted()
    synchronized{
      while (mulliganing) {
        val timeLeft = Duel.SECONDS_TO_MULLIGAN - stopWatch.elapsed(TimeUnit.SECONDS)
        if (mulligansDone){
          processMulligans()
          mulliganing = false
        } else if (timeLeft > 0 )
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
        new CardDraw(activePlayerName,activePlayerName,null, cardDrawnChange.deckEnded),
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
            val attackers = gameState.attackers
            val attackerIDs = new Array[Int](attackers.size)
            attackers.indices.foreach(i => attackerIDs(i) = attackers(i).id)
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
            val defenses = gameState.defenses
            val defenseIDs = new Array[Array[Int]](defenses.size)
            for (i <- defenses.indices) {
              defenseIDs(i) = new Array[Int](defenses(i).size)
              for (j <- defenses(i).indices)
                defenseIDs(i)(j) = defenses(i)(j).id
            }
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
      gameState.defenses.foreach(defense => {
        (1 until defense.size).toStream.foreach(defenderIndex => {
          defense(0).changeLifeBy(0-defense(defenderIndex).power)
          defense(defenderIndex).changeLifeBy(0-defense(0).power)
          battleChanges ++= Array(new SummonValueChange(defense(defenderIndex).id, GameChange.Value.LIFE, defense(defenderIndex).life))
        })
        if(defense.size > 1)
          battleChanges ++= Array(new SummonValueChange(defense(0).id, GameChange.Value.LIFE, defense(0).life))
        else{
          gameState.nonActivePlayer.changeLifeBy(0-defense(0).power)
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

      gameState.defenses.foreach(d => d.foreach(summon => {
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