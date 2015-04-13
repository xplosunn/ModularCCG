package unit.server.game.card.ability

import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import common.card.{Spell, Summon}
import org.junit.Assert._
import org.junit.Test
import server.game.card.{GameSpell, GameSummon}

/**
 * Created by xs on 04-10-2014.
 */
class SummonAbilityEffectLibraryTest extends AbilityLibraryTest{

  @Test
  def digger(){
    val testAbilityLibraryIndex = 0

    nap.pile.cards += new GameSummon(new Summon, nap, 400)
    nap.pile.cards += new GameSpell(new Spell, nap, 401)
    assertTrue(nap.pile.cards.size == 2)
    val napHandCardCount = nap.hand.cards.size

    for(cardsInPile <- 0 to 4)
      for(testAbilityLevel <- 1 to 3){
        val testSummon = new GameSummon(new Summon{
          assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
        }, ap, 300)

        ap.manaTotal = testSummon.cost
        ap.refillMana()
        assertTrue(ap.hand.cards.size == 4)
        ap.hand.cards += testSummon
        while(ap.pile.cards.size < cardsInPile)
          ap.pile.cards += new GameSummon(new Summon, ap, 402 + cardsInPile)
        assertTrue(ap.pile.cards.size == cardsInPile)

        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)

        assertTrue(nap.pile.cards.size == 2)
        assertTrue(ap.pile.cards.size == cardsInPile - Math.min(cardsInPile, testAbilityLevel))
        assertTrue(cardsInPile + " " + testAbilityLevel + " " + ap.hand.cards.size, ap.hand.cards.size == (4 + Math.min(testAbilityLevel, cardsInPile)))
        assertTrue(nap.hand.cards.size + " " + napHandCardCount, nap.hand.cards.size == napHandCardCount)

        ap.battlefield.summons -= testSummon
        ap.pile.cards.remove(0, ap.pile.cards.size)
        assertTrue(ap.pile.cards.size == 0)
        while(ap.hand.cards.size > 4)
          ap.hand.cards.remove(0)
        assertTrue(ap.pile.cards.size == 0)
      }
  }

  @Test
  def grenade(){
    val testAbilityLibraryIndex = 1
    val killSpell: GameSpell = new GameSpell(new Spell(){
      assertTrue(addAbility(5, 1))
    }, ap, 301)
    for(testAbilityLevel <- 1 to 3){
      val testSummon = new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
      }, ap, 300)

      ap.manaTotal = testSummon.card.cost + killSpell.cost
      ap.refillMana()
      assertTrue(ap.hand.cards.size == 4)
      ap.hand.cards += testSummon
      assertTrue(nap.lifeTotal == 30)

      testDuel.addCardToBePlayed(testSummon.id)
      Thread.sleep(processMillis)
      assertTrue(ap.battlefield.summons.contains(testSummon))
      assertTrue(ap.lifeTotal == 30)
      assertTrue(nap.lifeTotal == 30)

      ap.hand.cards += killSpell
      assertEquals(killSpell.cost, ap.availableMana)
      testDuel.addCardToBePlayed(killSpell.id)
      Thread.sleep(processMillis)
      assertTrue(ap.pile.cards.contains(killSpell))
      assertTrue(ap.pile.cards.contains(testSummon))
      assertTrue(ap.lifeTotal == 30)
      assertTrue(nap.lifeTotal + " " + testAbilityLevel*2, nap.lifeTotal == (30 - testAbilityLevel*2))

      ap.pile.cards -= testSummon
      ap.pile.cards -= killSpell
      assertTrue(ap.battlefield.summons.size == 0)
      assertTrue(ap.pile.cards.size == 0)
      nap.lifeTotal = 30
    }
  }

  @Test
  def poisonholder(){
    val testAbilityLibraryIndex = 2
    assertTrue(SummonAbilityLibrary.abilityList(2).timing == SummonAbility.ON_COMBAT)

    for(testAbilityLevel<- 1 to 3){
      for(attackingSummonsCount<- 1 to 5){
        val testSummon = new GameSummon(new Summon{
          assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
          assertTrue(SummonAbilityLibrary.abilityList(abilityLibraryIndex(0)).timing == SummonAbility.ON_COMBAT)
        }, ap, 300)
        ap.manaTotal = testSummon.cost
        ap.refillMana()

        val attackerArray = new Array[Int](attackingSummonsCount)
        for(i<-0 until attackingSummonsCount){
          nap.battlefield.summons += new GameSummon(new Summon, nap, 400 + i)
          attackerArray(i) = 400 + i
        }
        assertTrue(nap.battlefield.summons.size == attackingSummonsCount)

        ap.hand.cards += testSummon
        assertTrue(ap.hand.cards.contains(testSummon))
        assertTrue(ap.availableMana == testSummon.cost)
        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)
        assertTrue(testAbilityLevel + " " + ap.battlefield.summons.size, ap.battlefield.summons.size == 1)
        assertTrue(nap.battlefield.summons.size == attackingSummonsCount)
        testDuel.endTurn()
        Thread.sleep(processMillis)

        testDuel.nextStep()
        Thread.sleep(processMillis)

        testDuel.setAttackers(attackerArray)
        Thread.sleep(processMillis)

        testDuel.setDefenses(Array((400,300)))
        Thread.sleep(processMillis)

        assertTrue(ap.battlefield.summons.isEmpty)
        assertTrue(nap.battlefield.summons.size == attackingSummonsCount - Math.min(attackingSummonsCount, testAbilityLevel))

        ap.pile.cards.clear()
        nap.battlefield.summons.clear()
        testDuel.endTurn()
        Thread.sleep(processMillis)
      }
    }
  }

  @Test
  def evader(){
    val testAbilityLibraryIndex = 3

    for(testAbilityLevel<- 1 to 3){
      val undefendedEvader = new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
      }, ap, 300)
      val defendedEvader = new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
        life(2)
      }, ap, 301)
      val defender = new GameSummon(new Summon{}, nap, 400)

      ap.battlefield.summons += undefendedEvader
      ap.battlefield.summons += defendedEvader
      nap.battlefield.summons += defender

      testDuel.nextStep()
      Thread.sleep(processMillis)
      testDuel.setAttackers(Array(undefendedEvader.id, defendedEvader.id))
      Thread.sleep(processMillis)
      assertTrue(nap.lifeTotal == 30)
      testDuel.setDefenses(Array((defendedEvader.id, defender.id)))
      Thread.sleep(processMillis)

      assertTrue(testAbilityLevel + " " + nap.lifeTotal, nap.lifeTotal == 29)
      assertTrue(undefendedEvader.power == 1 + 2*testAbilityLevel)
      assertTrue(undefendedEvader.life == 1 + 2*testAbilityLevel)
      assertTrue(defendedEvader.power == 1)
      assertTrue(defendedEvader.life == 1)

      ap.battlefield.summons.clear()
      nap.lifeTotal = 30
      testDuel.endTurn()
      Thread.sleep(processMillis)
      testDuel.endTurn()
      Thread.sleep(processMillis)
    }
  }

  @Test
  def warlord(){
    val testAbilityLibraryIndex = 4

    for(testAbilityLevel<- 1 to 3){
      val testSummon = new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
      }, ap, 300)
      for(otherSummons<-0 to 4){
        ap.manaTotal = testSummon.cost
        ap.refillMana()
        for(i<-0 until otherSummons)
          ap.battlefield.summons += new GameSummon(new Summon{changePowerBy(i); changeLifeBy(2*i)}, ap, 400 + i)

        for(i<-0 until otherSummons){
          assertEquals(1, ap.battlefield.summons.count(s => (s.power == 1 + i) && (s.life == 1 + 2*i)))
        }
        ap.hand.cards += testSummon
        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)
        assertTrue(ap.battlefield.summons.count(s => s.power == 1 && s.life == 1) == 1)
        assertTrue(ap.battlefield.summons.count(s => s.power == 1 && s.life == 3) == 0)
        for(i<-0 until otherSummons)
          assertTrue(ap.battlefield.summons.count(s => (s.power == 1 + i + testAbilityLevel) && (s.life == 1 + 2*i + testAbilityLevel)) == 1)

        ap.battlefield.summons.clear()
      }
    }

  }

  @Test
  def champion(){
    val testAbilityLibraryIndex = 5

    for(testAbilityLevel<- 1 to 3){
      val testSummon = new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
      }, ap, 300)
      for(otherSummons<-0 to 4){
        ap.manaTotal = testSummon.cost
        ap.refillMana()
        for(i<-0 until otherSummons)
          ap.battlefield.summons += new GameSummon(new Summon, ap, 400 + i)

        ap.hand.cards += testSummon
        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)
        if(otherSummons != 0)
          assertTrue(ap.battlefield.summons.count(s => s.power == 1 && s.life == 1) == otherSummons)
        else
          assertTrue(ap.battlefield.summons.count(s => s.power == 1 && s.life == 1) == 1)
        assertTrue(testAbilityLevel + " " + otherSummons + " " + testSummon.power, testSummon.power == testSummon.life && testSummon.power == 1+ testAbilityLevel*otherSummons)

        testSummon.restoreOriginalPowerAndLife()
        ap.battlefield.summons.clear()
      }
    }

  }
}
