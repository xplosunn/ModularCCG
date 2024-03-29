package server.game.card.ability

import common.card.ability.change._
import common.game.RemoteCard
import server.game.card.{BattlefieldSummon, GameCard, GameSummon}
import server.game.{GameState, Player}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object SpellAbilityEffectLibrary {
  class SpellAbilityEffect(changesBattleFieldState: Boolean,
                           val apply: (Int, GameState) => ArrayBuffer[GameChange])
    extends CardAbilityEffect(changesBattleFieldState){}

  val effects = new Array[SpellAbilityEffect](6)

  effects(0) = new SpellAbilityEffect(true, (level: Int, state: GameState) => {
    val retn = new ArrayBuffer[GameChange]()
    state.players.foreach((p: Player) => {
      p.battlefield.foreach((s: BattlefieldSummon) => {
        s.changeLifeBy(0 - level)
        s.changePowerBy(0 - level)
        retn += new SummonValueChange(s.id, GameChange.Value.LIFE, s.life)
        retn += new SummonValueChange(s.id, GameChange.Value.POWER, s.power)
      })
    })
    retn
  })

  effects(1) = new SpellAbilityEffect(false, (level: Int, state: GameState) => {
    val retn = new ArrayBuffer[GameChange]()
    val player = state.activePlayer.handler.userName
    for (i <- 0 until level){
      retn += state.activePlayer.drawCard
      retn += state.activePlayer.drawCard
    }
    retn
  })

  effects(2) = new SpellAbilityEffect(false, (level: Int, state: GameState) => {
    val retn = new ArrayBuffer[GameChange]()
    state.activePlayer.manaTotal += level
    retn += new PlayerValueChange(state.activePlayer.handler.userName, GameChange.Value.MANA, state.activePlayer.manaTotal)
    retn
  })

  effects(3) = new SpellAbilityEffect(true, (level: Int, state: GameState) => {
    val retn = new ArrayBuffer[GameChange]()
    var summons = new ArrayBuffer[BattlefieldSummon]()
    summons ++= state.nonActivePlayer.battlefield
    for (i <- 0 until level)
      if (summons.size > 0) {
        val summonToKill = summons(new Random().nextInt(summons.size))
        retn += new SummonValueChange(summonToKill.id, GameChange.Value.LIFE, 0)
        summonToKill.setZeroLife()
        summons -= summonToKill
      }
    retn
  })

  effects(4) = new SpellAbilityEffect(false, (level: Int, state: GameState) => {
    val retn = new ArrayBuffer[GameChange]()
    val deck = state.nonActivePlayer.deck.cards
    if (deck.size > 0)
      for (i <- 0 until level) {
        val remoteCard = new RemoteCard(state.nextCardIdCallback(), state.activePlayer.handler.userName, deck(new Random().nextInt(deck.size)).card)
        state.activePlayer.hand += GameCard.build(remoteCard.card, state.activePlayer, remoteCard.id)
        retn += new NewCard(remoteCard, GameChange.Zone.HAND)
      }
    retn
  })

  effects(5) = new SpellAbilityEffect(true, (level: Int, state: GameState) => {
    val retn = new ArrayBuffer[GameChange]()
    state.players.foreach( p =>{
      val summons = new ArrayBuffer[BattlefieldSummon]() ++ p.battlefield
      for (_ <- 0 until level)
        if (summons.size > 0) {
          val summonToKill = summons(new Random().nextInt(summons.size))
          retn += new SummonValueChange(summonToKill.id, GameChange.Value.LIFE, 0)
          summonToKill.setZeroLife()
          summons -= summonToKill
        }
    })
    retn
  })
}