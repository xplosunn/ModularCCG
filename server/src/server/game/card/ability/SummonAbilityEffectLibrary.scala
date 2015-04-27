package server.game.card.ability

import common.card.ability.change.{GameChange, PlayerValueChange, SummonValueChange, ZoneChange}
import server.game.card.{BattlefieldSummon, GameSummon}
import server.game.{GameState, Player}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object SummonAbilityEffectLibrary {
  class SummonAbilityEffect(changesBattleFieldState: Boolean,
                            val apply: (Int, GameState, BattlefieldSummon) => ArrayBuffer[GameChange])
    extends CardAbilityEffect(changesBattleFieldState){}

  val effects = new Array[SummonAbilityEffect](6)

  effects(0) = new SummonAbilityEffect(false, (level: Int, state: GameState, invoker: BattlefieldSummon) => {
    val retn = new ArrayBuffer[GameChange]()
    (0 until level).toStream.takeWhile(_ => invoker.owner.pile.size > 0).foreach(i=>{
      val card = invoker.owner.pile(new Random().nextInt(invoker.owner.pile.size))
      invoker.owner.pile -= card
      invoker.owner.hand += card
      retn += new ZoneChange(card.id,invoker.owner.handler.userName,GameChange.Zone.HAND)
    })
    retn
  })

  effects(1) = new SummonAbilityEffect(false, (level: Int, state: GameState, invoker: BattlefieldSummon) => {
    val retn = new ArrayBuffer[GameChange]()
    state.players.foreach( (p: Player)=>{
      if(p != invoker.owner){
        p.changeLifeBy(0-2*level)
        retn += new PlayerValueChange(p.handler.userName,GameChange.Value.LIFE,p.lifeTotal)
      }
    })
    retn
  })

  effects(2) = new SummonAbilityEffect(true, (level: Int, state: GameState, invoker: BattlefieldSummon) => {
    val retn = new ArrayBuffer[GameChange]()
    val blockedSummon = state.attackerOf(invoker)
    if (blockedSummon != null) {
      blockedSummon.setZeroLife()
      retn += new SummonValueChange(blockedSummon.id, GameChange.Value.LIFE, 0)
      var attackersLeft = new ArrayBuffer[BattlefieldSummon]
      state.attackers.foreach(attacker => if(attacker.life > 0) attackersLeft += attacker)

      (0 until level - 1).toStream.takeWhile(_ => attackersLeft.size > 0).foreach(i => {
        val killedSummon = attackersLeft(new Random().nextInt(attackersLeft.size))
        killedSummon.setZeroLife()
        attackersLeft -= killedSummon
        retn += new SummonValueChange(killedSummon.id, GameChange.Value.LIFE, 0)
      })
    }
    retn
  })

  effects(3) = new SummonAbilityEffect(false, (level: Int, state: GameState, invoker: BattlefieldSummon) => {
    val retn = new ArrayBuffer[GameChange]()
    if(!state.hasDefenders(invoker)){
      invoker.changeLifeBy(2*level)
      invoker.changePowerBy(2*level)
      retn += new SummonValueChange(invoker.id,GameChange.Value.LIFE,invoker.life)
      retn += new SummonValueChange(invoker.id,GameChange.Value.POWER,invoker.power)
    }
    retn
  })

  effects(4) = new SummonAbilityEffect(false, (level: Int, state: GameState, invoker: BattlefieldSummon) => {
    val retn = new ArrayBuffer[GameChange]()
    invoker.owner.battlefield.foreach(summon => {
      if(summon != invoker){
        summon.changePowerBy(level)
        summon.changeLifeBy(level)
        retn += new SummonValueChange(summon.id,GameChange.Value.POWER,summon.power)
        retn += new SummonValueChange(summon.id,GameChange.Value.LIFE,summon.life)
      }
    })
    retn
  })

  effects(5) = new SummonAbilityEffect(false, (level: Int, state: GameState, invoker: BattlefieldSummon) => {
    val retn = new ArrayBuffer[GameChange]()
    val valueAdded = (invoker.owner.battlefield.size - 1) * level
    invoker.changePowerBy(valueAdded)
    invoker.changeLifeBy(valueAdded)
    retn += new SummonValueChange(invoker.id, GameChange.Value.POWER, invoker.power)
    retn += new SummonValueChange(invoker.id, GameChange.Value.LIFE, invoker.life)
    retn
  })
}
