package server.game

import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import server.game.card.GameSummon
import server.game.exception.{GameTiedException, PlayerWonException}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class GameState (val players: Array[Player]) {
  if(players.length > 2)
    throw new IllegalArgumentException
  
  var activePlayerIndex: Int = new Random().nextInt(players.size)
  //Holds all the summons up for combat each of the turns. The first summon in each array is the attacker
  //and the rest are blockers.
  private var battleSummons = new ArrayBuffer[ArrayBuffer[GameSummon]]
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
    battleSummons = new ArrayBuffer[ArrayBuffer[GameSummon]]
    val turnOwner = players(activePlayerIndex)
    turnOwner.manaTotal += 1
    turnOwner.refillMana()
  }

  def defendersOf(s: GameSummon): ArrayBuffer[GameSummon] = {
    val res = new ArrayBuffer[GameSummon]()
    for(battleArray<-battleSummons){
      if(battleArray(0) == s){
       for(j<-1 until battleArray.size)
         res += battleArray(j)
      }
    }
    res
  }

  def attackerOf(s: GameSummon): GameSummon = {
    for(battleArray<-battleSummons){
      for(j<- 1 until battleArray.size){
        if(battleArray(j) == s){
          return battleArray(0)
        }
      }
    }
    null
  }

  def attackers: ArrayBuffer[GameSummon] = {
    val summons = new ArrayBuffer[GameSummon]()
    for(i<-battleSummons.indices)
      summons += battleSummons(i)(0)
    summons
  }

  def attackerCount: Int = battleSummons.size

  def defenses: ArrayBuffer[ArrayBuffer[GameSummon]] = battleSummons


  def setAttackers(attackers: ArrayBuffer[GameSummon]){
    synchronized{
      if(!attackersSetThisTurn){
        attackersSetThisTurn = true
        for(attacker <- attackers)
          battleSummons += ArrayBuffer(attacker)
      }
    }
  }

  def attackersSet = attackersSetThisTurn

  def setDefenders(defenses: ArrayBuffer[ArrayBuffer[GameSummon]]){
    synchronized{
      if(!defendersSetThisTurn){
        defendersSetThisTurn = true
        for(defenseArray <- defenses)
          for(battleArray <- battleSummons)
            if(battleArray(0) == defenseArray(0))
              for(i<- 1 until defenseArray.size)
                battleArray += defenseArray(i)
      }
    }
  }

  def defendersSet = defendersSetThisTurn

  def hasDefenders(summon: GameSummon): Boolean = {
    for(battleArray<-battleSummons)
      if(battleArray(0) == summon && battleArray.size > 1)
        return true
    false
  }

  def nextDeathTrigger: (GameSummon, Int, Int) = {
    if(deathTriggers.size > 0){
      return deathTriggers.remove(0)
    }
    null
  }
  
  def checkBattlefieldState() {
    players.foreach(player => {
      val summonsToKill = new ArrayBuffer[GameSummon]()
      player.battlefield.summons.foreach(summon =>
        if (summon.life <= 0)
          summonsToKill += summon
        )
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