package unit.server.game.card.ability

import java.util.Random

import common.card.{Spell, Summon}
import common.game.GameSteps
import org.junit.Assert._
import org.junit.Test
import server.game.card.{GameSummon, GameSpell}
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
          randomPlayer.battlefield.cards += new GameSummon(new Summon {
            changePowerBy(2 * i)
            changeLifeBy(i)
          }, randomPlayer, 400 + i)
        }
        assertTrue(ap.battlefield.summons.size +
          nap.battlefield.summons.size == summonsInPlay)
        for(i<-0 until summonsInPlay){
          assertTrue(
            ap.battlefield.summons.count(s => (s.power == 1 + 2*i) && (s.life == 1 + i)) +
            nap.battlefield.summons.count(s => (s.power == 1 + 2*i) && (s.life == 1 + i)) == 1
          )
          assertTrue(
            ap.battlefield.summons.count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i)) +
              nap.battlefield.summons.count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i)) == 1
          )
        }

        ap.manaTotal = testSpell.cost
        ap.refillMana()

        ap.hand.cards += testSpell
        assertTrue(ap.hand.cards.contains(testSpell))
        testDuel.addCardToBePlayed(testSpell.id)
        Thread.sleep(processMillis)
        assertEquals(1, ap.pile.cards.count(c => c.isInstanceOf[GameSpell]))

        for(i<-0 until summonsInPlay){
          if(testAbilityLevel > i){
            val deadCount = ap.pile.summons.count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i)) +
              nap.pile.summons.count(s => (s.card.power == 1 + 2*i) && (s.card.life == 1 + i))
            assertTrue(testAbilityLevel + " " + summonsInPlay + " " + i + " " + deadCount, deadCount== 1)
          } else{
            val aliveSum = ap.battlefield.summons.count(s => (s.power == 1 + 2*i - testAbilityLevel) && (s.life == 1 + i - testAbilityLevel)) +
              nap.battlefield.summons.count(s => (s.power == 1 + 2*i - testAbilityLevel) && (s.life == 1 + i - testAbilityLevel))
            assertTrue(testAbilityLevel +" "+ summonsInPlay + " " + i + " " + aliveSum,aliveSum == 1)
          }
        }

        ap.battlefield.cards.clear()
        nap.battlefield.cards.clear()
        ap.pile.cards.clear()
        nap.pile.cards.clear()
      }
    }
  }

  @Test
  def drawCard(){
    val testAbilityLibraryIndex = 1
    for(testAbilityLevel <- 1 to 3){
      val testSpell = new GameSpell(new Spell{addAbility(testAbilityLibraryIndex, testAbilityLevel)}, ap, 300)

      val startingDeckCards = ap.deck.cards.size
      ap.hand.cards.clear()
      ap.hand.cards += testSpell
      ap.manaTotal = testSpell.cost
      ap.refillMana()

      assertEquals(ap.availableMana, testSpell.cost)
      testDuel.addCardToBePlayed(testSpell.id)
      Thread.sleep(processMillis)
      assertEquals(false, ap.hand.cards.contains(testSpell))
      assertTrue(ap.pile.cards.contains(testSpell))
      assertTrue(ap.hand.cards.size + " " + testAbilityLevel, ap.hand.cards.size == 2*testAbilityLevel)
      assertTrue(ap.hand.cards.count(gc => gc.owner == ap) == 2*testAbilityLevel)

      assertTrue(ap.pile.cards.size == 1)
      assertTrue(ap.deck.cards.size == startingDeckCards - 2*testAbilityLevel)
      ap.pile.cards.clear()
    }
  }

  @Test
  def drawCardFromEmptyDeck(){
    ap.deck.cards.clear()
    val testSpell = new GameSpell(new Spell{addAbility(1, 1)}, ap, 300)
    assertEquals(30, ap.lifeTotal)
    ap.manaTotal = testSpell.cost
    ap.refillMana()
    ap.hand.cards.clear()
    ap.hand.cards += testSpell
    testDuel.addCardToBePlayed(testSpell.id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(ap.pile.cards.contains(testSpell))
    assertEquals(22,ap.lifeTotal)
  }

  @Test
  def manaUp(){
    val testAbilityLibraryIndex = 2

    for(testAbilityLevel <- 1 to 3) {
      val testSpell = new GameSpell(new Spell {
        addAbility(testAbilityLibraryIndex, testAbilityLevel)
      }, ap, 300)
      for(startingMana <- testSpell.cost to 20){
        ap.manaTotal = startingMana
        ap.refillMana()

        ap.hand.cards += testSpell
        assertTrue(ap.hand.cards.contains(testSpell))
        assertEquals(1, ap.hand.cards.count(gc => gc.id == testSpell.id))
        testDuel.addCardToBePlayed(testSpell.id)
        assertTrue(testDuel.isAlive)
        assertEquals(GameSteps.MAIN_1st, testDuel.currentTurn.currentStep)
        assertEquals(ap, testDuel.getGameState.activePlayer)
        Thread.sleep(processMillis)

        assertEquals(GameSteps.MAIN_1st, testDuel.currentTurn.currentStep)
        assertEquals(ap, testDuel.getGameState.activePlayer)
        assertTrue(testDuel.isAlive)

        assertTrue(ap.pile.cards.contains(testSpell))
        assertTrue(ap.pile.cards.size == 1)
        assertTrue(startingMana + " " + testAbilityLevel + " " + ap.manaTotal, ap.manaTotal == startingMana + testAbilityLevel)
        ap.pile.cards.clear()
      }
    }
  }

  @Test
  def killRandomSummon(){
    val testAbilityLibraryIndex = 3
    assertTrue(ap.battlefield.cards.size == 0)
    ap.battlefield.cards += new GameSummon(new Summon, ap, 500)

    for(testAbilityLevel <- 1 to 3) {
      val testSpell = new GameSpell(new Spell {
        addAbility(testAbilityLibraryIndex, testAbilityLevel)
      }, ap, 300)
      for(summonsInPlay <- 0 to 6){
        ap.manaTotal = testSpell.cost
        ap.refillMana()
        ap.hand.cards += testSpell


        assertTrue(ap.battlefield.cards.size == 1)
        for(i<- 0 until summonsInPlay){
          nap.battlefield.cards += new GameSummon(new Summon{changeLifeBy(new Random().nextInt(6))}, nap, 400+i)
        }
        assertTrue(nap.battlefield.cards.size == summonsInPlay)

        testDuel.addCardToBePlayed(testSpell.id)
        Thread.sleep(processMillis)
        assertTrue(ap.battlefield.cards.size == 1)
        assertTrue(nap.battlefield.cards.size == summonsInPlay - Math.min(summonsInPlay, testAbilityLevel))

        nap.battlefield.cards.clear()
      }
    }
  }

  @Test
  def drawCardFromRandomOpponent(){
    val testAbilityLibraryIndex = 4
    for(testAbilityLevel <- 1 to 3){
      val testSpell = new GameSpell(new Spell{addAbility(testAbilityLibraryIndex, testAbilityLevel)}, ap, 300)

      val startingDeckCards = nap.deck.cards.size
      ap.hand.cards.clear()
      ap.hand.cards += testSpell
      ap.manaTotal = testSpell.cost
      ap.refillMana()

      assertTrue(ap.hand.cards.size == 1)
      testDuel.addCardToBePlayed(testSpell.id)
      Thread.sleep(processMillis)
      assertTrue(ap.hand.cards.size == testAbilityLevel)
      assertTrue(ap.hand.cards.count( gc => gc.owner == nap) == testAbilityLevel)

      assertTrue(ap.pile.cards.size == 1)
      assertTrue(nap.deck.cards.size == startingDeckCards - testAbilityLevel)
      ap.pile.cards.clear()
    }
  }

  @Test
  def drawCardFromRandomOpponentEmptyDeck(){
    nap.deck.cards.clear()
    val testSpell = new GameSpell(new Spell{addAbility(4, 1)}, ap, 300)
    assertEquals(30, ap.lifeTotal)
    ap.manaTotal = testSpell.cost
    ap.refillMana()
    ap.hand.cards.clear()
    ap.hand.cards += testSpell
    testDuel.addCardToBePlayed(testSpell.id)
    Thread.sleep(UnitTestConstants.processMillis)
    assertTrue(ap.pile.cards.contains(testSpell))
    assertEquals(26,ap.lifeTotal)
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
          ap.hand.cards += testSpell
          ap.manaTotal = testSpell.cost
          ap.refillMana()

          for(i<- 0 until apStartingSummons)
            ap.battlefield.cards += new GameSummon(new Summon, ap, 400+i)
          for(i<- 0 until napStartingSummons)
            nap.battlefield.cards += new GameSummon(new Summon, nap, 500+i)

          assertTrue(ap.battlefield.cards.size == apStartingSummons)
          assertTrue(nap.battlefield.cards.size == napStartingSummons)
          testDuel.addCardToBePlayed(testSpell.id)
          Thread.sleep(processMillis)

          assertTrue(ap.battlefield.cards.size == apStartingSummons - Math.min(apStartingSummons, testAbilityLevel))
          assertTrue(nap.battlefield.cards.size == napStartingSummons - Math.min(napStartingSummons, testAbilityLevel))
          ap.battlefield.cards.clear()
          nap.battlefield.cards.clear()

        }
      }
    }
  }
}