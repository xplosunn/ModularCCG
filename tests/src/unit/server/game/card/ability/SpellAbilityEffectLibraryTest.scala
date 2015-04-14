package unit.server.game.card.ability

import java.util.Random

import common.card.{Spell, Summon}
import common.game.GameSteps
import org.junit.Assert._
import org.junit.Test
import server.game.card.{BattlefieldSummon, GameSummon, GameSpell}
import unit.UnitTestConstants

class SpellAbilityEffectLibraryTest extends AbilityLibraryTest{

  @Test
  def decreaseSummonStats(){
    val testAbilityLibraryIndex = 0
    for(testAbilityLevel <- 1 to 3){
      val testSpell = new GameSpell(new Spell{addAbility(testAbilityLibraryIndex, testAbilityLevel)}, ap, 300)
      for(summonsInPlay <- 1 to 6){
        for(i<-0 until summonsInPlay){
          val randomPlayer = testDuel.getGameState.players(new Random().nextInt(2))
          randomPlayer.battlefield += new BattlefieldSummon(new GameSummon(new Summon {
            changePowerBy(2 * i)
            changeLifeBy(i)
          }, randomPlayer, 400 + i))
        }
        assertTrue(ap.battlefield.size +
          nap.battlefield.size == summonsInPlay)
        for(i<-0 until summonsInPlay){
          assertTrue(
            ap.battlefield.count(s => (s.power == 1 + 2*i) && (s.life == 1 + i)) +
            nap.battlefield.count(s => (s.power == 1 + 2*i) && (s.life == 1 + i)) == 1
          )
          assertTrue(
            ap.battlefield.count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i)) +
              nap.battlefield.count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i)) == 1
          )
        }

        ap.manaTotal = testSpell.cost
        ap.refillMana()

        ap.hand += testSpell
        assertTrue(ap.hand.contains(testSpell))
        testDuel.addCardToBePlayed(testSpell.id)
        Thread.sleep(processMillis)
        assertEquals(1, ap.pile.count(c => c.isInstanceOf[GameSpell]))

        for(i<-0 until summonsInPlay){
          if(testAbilityLevel > i){
            val deadCount = ap.pile.collect({case gc: GameSummon => gc}).count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i)) +
              nap.pile.collect({case gc: GameSummon => gc}).count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i))
            assertTrue(testAbilityLevel + " " + summonsInPlay + " " + i + " " + deadCount, deadCount== 1)
          } else{
            val aliveSum = ap.battlefield.count(s => (s.power == 1 + 2*i - testAbilityLevel) && (s.life == 1 + i - testAbilityLevel)) +
              nap.battlefield.count(s => (s.power == 1 + 2*i - testAbilityLevel) && (s.life == 1 + i - testAbilityLevel))
            assertTrue(testAbilityLevel +" "+ summonsInPlay + " " + i + " " + aliveSum,aliveSum == 1)
          }
        }

        ap.battlefield.clear()
        nap.battlefield.clear()
        ap.pile.clear()
        nap.pile.clear()
      }
    }
  }

  @Test
  def drawCard(){
    val testAbilityLibraryIndex = 1
    for(testAbilityLevel <- 1 to 3){
      val testSpell = new GameSpell(new Spell{addAbility(testAbilityLibraryIndex, testAbilityLevel)}, ap, 300)

      val startingDeckCards = ap.deck.cards.size
      ap.hand.clear()
      ap.hand += testSpell
      ap.manaTotal = testSpell.cost
      ap.refillMana()

      assertEquals(ap.availableMana, testSpell.cost)
      testDuel.addCardToBePlayed(testSpell.id)
      Thread.sleep(processMillis)
      assertEquals(false, ap.hand.contains(testSpell))
      assertTrue(ap.pile.contains(testSpell))
      assertTrue(ap.hand.size + " " + testAbilityLevel, ap.hand.size == 2*testAbilityLevel)
      assertTrue(ap.hand.count(gc => gc.owner == ap) == 2*testAbilityLevel)

      assertTrue(ap.pile.size == 1)
      assertTrue(ap.deck.cards.size == startingDeckCards - 2*testAbilityLevel)
      ap.pile.clear()
    }
  }

  @Test
  def drawCardFromEmptyDeck(){
    ap.deck.cards.clear()
    val testSpell = new GameSpell(new Spell{addAbility(1, 1)}, ap, 300)
    assertEquals(30, ap.lifeTotal)
    ap.manaTotal = testSpell.cost
    ap.refillMana()
    ap.hand.clear()
    ap.hand += testSpell
    testDuel.addCardToBePlayed(testSpell.id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(ap.pile.contains(testSpell))
    assertEquals(22,ap.lifeTotal)
  }

  @Test
  def manaUp(){
    val testAbilityLibraryIndex = 2

    for(testAbilityLevel <- 1 to 3) {
      val testSpell = new GameSpell(new Spell {
        addAbility(testAbilityLibraryIndex, testAbilityLevel)
      }, ap, 300)
      for(startingMana <- testSpell.cost to 10){
        ap.manaTotal = startingMana
        ap.refillMana()

        ap.hand += testSpell
        assertTrue(ap.hand.contains(testSpell))
        assertEquals(1, ap.hand.count(gc => gc.id == testSpell.id))
        testDuel.addCardToBePlayed(testSpell.id)
        assertTrue(testDuel.isAlive)
        assertEquals(GameSteps.MAIN_1st, testDuel.currentTurn.currentStep)
        assertEquals(ap, testDuel.getGameState.activePlayer)
        Thread.sleep(processMillis)

        assertEquals(GameSteps.MAIN_1st, testDuel.currentTurn.currentStep)
        assertEquals(ap, testDuel.getGameState.activePlayer)
        assertTrue(testDuel.isAlive)

        assertTrue(ap.pile.contains(testSpell))
        assertTrue(ap.pile.size == 1)
        assertTrue(startingMana + " " + testAbilityLevel + " " + ap.manaTotal, ap.manaTotal == startingMana + testAbilityLevel)
        ap.pile.clear()
      }
    }
  }

  @Test
  def killRandomSummon(){
    val testAbilityLibraryIndex = 3
    assertTrue(ap.battlefield.size == 0)
    ap.battlefield += new BattlefieldSummon(new GameSummon(new Summon, ap, 500))

    for(testAbilityLevel <- 1 to 3) {
      val testSpell = new GameSpell(new Spell {
        addAbility(testAbilityLibraryIndex, testAbilityLevel)
      }, ap, 300)
      for(summonsInPlay <- 0 to 6){
        ap.manaTotal = testSpell.cost
        ap.refillMana()
        ap.hand += testSpell


        assertTrue(ap.battlefield.size == 1)
        for(i<- 0 until summonsInPlay){
          nap.battlefield += new BattlefieldSummon(new GameSummon(new Summon{changeLifeBy(new Random().nextInt(6))}, nap, 400+i))
        }
        assertTrue(nap.battlefield.size == summonsInPlay)

        testDuel.addCardToBePlayed(testSpell.id)
        Thread.sleep(processMillis)
        assertTrue(ap.battlefield.size == 1)
        assertTrue(nap.battlefield.size == summonsInPlay - Math.min(summonsInPlay, testAbilityLevel))

        nap.battlefield.clear()
      }
    }
  }

  @Test
  def copyCardFromRandomOpponent(){
    val testAbilityLibraryIndex = 4
    for(testAbilityLevel <- 1 to 3){
      val testSpell = new GameSpell(new Spell{addAbility(testAbilityLibraryIndex, testAbilityLevel)}, ap, 300)

      val startingDeckCards = nap.deck.cards.size
      ap.hand.clear()
      ap.hand += testSpell
      ap.manaTotal = testSpell.cost
      ap.refillMana()

      assertTrue(ap.hand.size == 1)
      testDuel.addCardToBePlayed(testSpell.id)
      Thread.sleep(processMillis)
      assertEquals(testAbilityLevel, ap.hand.size)
      assertTrue(ap.hand.count( gc => gc.owner == ap) == testAbilityLevel)

      assertTrue(ap.pile.size == 1)
      assertTrue(nap.deck.cards.size == startingDeckCards)
      ap.pile.clear()
    }
  }

  @Test
  def copyCardFromRandomOpponentEmptyDeck(){
    nap.deck.cards.clear()
    val testSpell = new GameSpell(new Spell{addAbility(4, 1)}, ap, 300)
    assertEquals(30, ap.lifeTotal)
    ap.manaTotal = testSpell.cost
    ap.refillMana()
    ap.hand.clear()
    ap.hand += testSpell
    testDuel.addCardToBePlayed(testSpell.id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(ap.pile.contains(testSpell))
    assertTrue(ap.hand.isEmpty)
    assertEquals(30, ap.lifeTotal)
  }

  @Test
  def killEachPlayerSummon() {
    val testAbilityLibraryIndex = 5
    for (testAbilityLevel <- 1 to 3) {
      val testSpell = new GameSpell(new Spell {
        addAbility(testAbilityLibraryIndex, testAbilityLevel)
      }, ap, 300)
      for (apStartingSummons <- 0 to 5) {
        for (napStartingSummons <- 0 to 5) {
          ap.hand += testSpell
          ap.manaTotal = testSpell.cost
          ap.refillMana()

          for(i<- 0 until apStartingSummons)
            ap.battlefield += new BattlefieldSummon(new GameSummon(new Summon, ap, 400+i))
          for(i<- 0 until napStartingSummons)
            nap.battlefield += new BattlefieldSummon(new GameSummon(new Summon, nap, 500+i))

          assertEquals(apStartingSummons, ap.battlefield.size)
          assertEquals(napStartingSummons, nap.battlefield.size)
          testDuel.addCardToBePlayed(testSpell.id)
          Thread.sleep(processMillis)

          assertEquals(napStartingSummons - Math.min(napStartingSummons, testAbilityLevel), nap.battlefield.size)
          assertEquals(apStartingSummons - Math.min(apStartingSummons, testAbilityLevel), ap.battlefield.size)
          ap.battlefield.clear()
          nap.battlefield.clear()

        }
      }
    }
  }
}