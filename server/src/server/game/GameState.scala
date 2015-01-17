package server.game

import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import server.game.card.GameSummon
import server.game.exception.{GameTiedException, PlayerWonException}

import scala.collection.mutable._
import scala.util.Random

class GameState (val players: Array[Player], val game: Game) {
  if(players.length > 2)
    throw new IllegalArgumentException
  
  var activePlayerIndex: Int = new Random().nextInt(players.size)
  //Holds all the summons up for combat each of the turns. The first summon in each array is the attacker
  //and the rest are blockers.
  private val battleSummons = new HashMap[GameSummon, Set[GameSummon]] with MultiMap[GameSummon, GameSummon]
  //Holds the death triggers that haven't been processed yet. It couples the invoker with the index of
  //the ability on the SummonAbilityLibrary and the level of the ability
  private var deathTriggers = new ArrayBuffer[(GameSummon, Int, Int)]
  private var attackersSetThisTurn = false
  private var defendersSetThisTurn = false

  def activePlayer = players(activePlayerIndex)
  def nonActivePlayer: Player = players(1-activePlayerIndex)

  def checkGameState() {
    checkBattlefieldState()
    checkPlayerState()
  }

  def nextTurn(){
    activePlayerIndex = (activePlayerIndex + 1) % players.size
    attackersSetThisTurn = false
    defendersSetThisTurn = false
    battleSummons.clear()
    val turnOwner = players(activePlayerIndex)
    turnOwner.manaTotal += 1
    turnOwner.refillMana()
  }

  def defendersOf(s: GameSummon): Set[GameSummon] = {
    battleSummons.get(s).get
  }

  def attackerOf(s: GameSummon): GameSummon = {
    battleSummons.collectFirst({case (gs, set) if set.contains(s) => gs}).get
  }

  def attackers: collection.Set[GameSummon] = {
    battleSummons.keySet
  }

  def attackerCount: Int = battleSummons.size

  def defenses = battleSummons

  def setAttackers(attackers: ArrayBuffer[GameSummon]){
    synchronized{
      if(!attackersSetThisTurn){
        attackersSetThisTurn = true
        attackers.foreach(attacker => battleSummons.put(attacker, Set[GameSummon]()))
      }
    }
  }

  def attackersSet = attackersSetThisTurn

  def setDefenders(defenses: Array[(GameSummon, GameSummon)]){
    synchronized{
      if(!defendersSetThisTurn){
        defendersSetThisTurn = true
        defenses.foreach(defense => battleSummons.addBinding(defense._1, defense._2))
      }
    }
  }

  def defendersSet = defendersSetThisTurn

  def hasDefenders(summon: GameSummon): Boolean = {
    battleSummons.get(summon).get.size > 0
  }

  def nextDeathTrigger: (GameSummon, Int, Int) = {
    if(deathTriggers.size > 0){
      return deathTriggers.remove(0)
    }
    null
  }
  
  def checkBattlefieldState() {
    players.foreach(player => {
      val summonsToKill = player.battlefield.summons.filter(gs => gs.life <= 0)

      player.battlefield.cards --= summonsToKill
      player.pile.cards ++= summonsToKill
      summonsToKill.foreach(summon => {
        (0 until summon.card.MAXIMUM_ABILITIES).toStream.takeWhile(i => summon.card.abilityLevel(i) != -1).foreach(
        i=> {
          val abilityIndex = summon.card.abilityLibraryIndex(i)
          if (SummonAbilityLibrary.abilityList(abilityIndex).timing == SummonAbility.ON_DEATH)
            deathTriggers += ((summon, abilityIndex, summon.card.abilityLevel(i)))
        })
      })
    })
  }
  
  def checkPlayerState() {
    players.count(p => p.lifeTotal <= 0) match {
      case 0 =>
      case 1 => throw new PlayerWonException(players.filter(p2 => p2.lifeTotal > 0)(0).handler.getUserName)
      case 2 => throw new GameTiedException
    }
  }
}