package unit.server.game.card.ability

import common.card.ability.{SummonAbility, SummonAbilityLibrary}
import common.card.{Spell, Summon}
import org.junit.Assert._
import org.junit.Test
import server.game.card.{BattlefieldSummon, GameSpell, GameSummon}

/**
 * Created by xs on 04-10-2014.
 */
class SummonAbilityEffectLibraryTest extends AbilityLibraryTest{

  @Test
  def digger(){
    val testAbilityLibraryIndex = 0

    nap.pile += new GameSummon(new Summon, nap, 400)
    nap.pile += new GameSpell(new Spell, nap, 401)
    assertTrue(nap.pile.size == 2)
    val napHandCardCount = nap.hand.size

    for(cardsInPile <- 0 to 4)
      for(testAbilityLevel <- 1 to 3){
        val testSummon = new GameSummon(new Summon{
          assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
        }, ap, 300)

        ap.manaTotal = testSummon.cost
        ap.refillMana()
        assertTrue(ap.hand.size == 4)
        ap.hand += testSummon
        while(ap.pile.size < cardsInPile)
          ap.pile += new GameSummon(new Summon, ap, 402 + cardsInPile)
        assertTrue(ap.pile.size == cardsInPile)

        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)

        assertTrue(nap.pile.size == 2)
        assertEquals(""+testAbilityLevel + " " + cardsInPile , cardsInPile - Math.min(cardsInPile, testAbilityLevel), ap.pile.size)
        assertTrue(cardsInPile + " " + testAbilityLevel + " " + ap.hand.size, ap.hand.size == (4 + Math.min(testAbilityLevel, cardsInPile)))
        assertTrue(nap.hand.size + " " + napHandCardCount, nap.hand.size == napHandCardCount)

        ap.battlefield --= ap.battlefield.collect({case s if s.id == testSummon.id => s})
        ap.pile.remove(0, ap.pile.size)
        assertTrue(ap.pile.size == 0)
        while(ap.hand.size > 4)
          ap.hand.remove(0)
        assertTrue(ap.pile.size == 0)
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
      assertTrue(ap.hand.size == 4)
      ap.hand += testSummon
      assertTrue(nap.lifeTotal == 30)

      testDuel.addCardToBePlayed(testSummon.id)
      Thread.sleep(processMillis)
      assertTrue(ap.battlefield.exists(bs => bs.id == testSummon.id))
      assertTrue(ap.lifeTotal == 30)
      assertTrue(nap.lifeTotal == 30)

      ap.hand += killSpell
      assertEquals(killSpell.cost, ap.availableMana)
      testDuel.addCardToBePlayed(killSpell.id)
      Thread.sleep(processMillis)
      assertTrue(ap.pile.contains(killSpell))
      assertTrue(ap.pile.contains(testSummon))
      assertTrue(ap.lifeTotal == 30)
      assertTrue(nap.lifeTotal + " " + testAbilityLevel*2, nap.lifeTotal == (30 - testAbilityLevel*2))

      ap.pile -= testSummon
      ap.pile -= killSpell
      assertTrue(ap.battlefield.size == 0)
      assertTrue(ap.pile.size == 0)
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
          nap.battlefield += new BattlefieldSummon(new GameSummon(new Summon, nap, 400 + i))
          attackerArray(i) = 400 + i
        }
        assertTrue(nap.battlefield.size == attackingSummonsCount)

        ap.hand += testSummon
        assertTrue(ap.hand.contains(testSummon))
        assertTrue(ap.availableMana == testSummon.cost)
        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)
        assertTrue(testAbilityLevel + " " + ap.battlefield.size, ap.battlefield.size == 1)
        assertTrue(nap.battlefield.size == attackingSummonsCount)
        testDuel.endTurn()
        Thread.sleep(processMillis)

        testDuel.nextStep()
        Thread.sleep(processMillis)

        testDuel.setAttackers(attackerArray)
        Thread.sleep(processMillis)

        testDuel.setDefenses(Array((400,300)))
        Thread.sleep(processMillis)

        assertTrue(ap.battlefield.isEmpty)
        assertTrue(nap.battlefield.size == attackingSummonsCount - Math.min(attackingSummonsCount, testAbilityLevel))

        ap.pile.clear()
        nap.battlefield.clear()
        testDuel.endTurn()
        Thread.sleep(processMillis)
      }
    }
  }

  @Test
  def evader(){
    val testAbilityLibraryIndex = 3

    for(testAbilityLevel<- 1 to 3){
      val undefendedEvader = new BattlefieldSummon(new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
      }, ap, 300))
      val defendedEvader = new BattlefieldSummon(new GameSummon(new Summon{
        assertTrue(addAbility(testAbilityLibraryIndex, testAbilityLevel))
        life(2)
      }, ap, 301))
      val defender = new BattlefieldSummon(new GameSummon(new Summon{}, nap, 400))

      ap.battlefield += undefendedEvader
      ap.battlefield += defendedEvader
      nap.battlefield += defender

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

      ap.battlefield.clear()
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
          ap.battlefield += new BattlefieldSummon(new GameSummon(new Summon{changePowerBy(i); changeLifeBy(2*i)}, ap, 400 + i))

        for(i<-0 until otherSummons){
          assertEquals(1, ap.battlefield.count(s => (s.power == 1 + i) && (s.life == 1 + 2*i)))
        }
        ap.hand += testSummon
        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)
        assertTrue(ap.battlefield.count(s => s.power == 1 && s.life == 1) == 1)
        assertTrue(ap.battlefield.count(s => s.power == 1 && s.life == 3) == 0)
        for(i<-0 until otherSummons)
          assertTrue(ap.battlefield.count(s => (s.power == 1 + i + testAbilityLevel) && (s.life == 1 + 2*i + testAbilityLevel)) == 1)

        ap.battlefield.clear()
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
          ap.battlefield += new BattlefieldSummon(new GameSummon(new Summon, ap, 400 + i))

        ap.hand += testSummon
        testDuel.addCardToBePlayed(testSummon.id)
        Thread.sleep(processMillis)
        val bfTestSummon = ap.battlefield.collect({case s: BattlefieldSummon if s.id == testSummon.id => s}).apply(0)
        assertNotNull(bfTestSummon)
        if(otherSummons != 0)
          assertTrue(ap.battlefield.count(s => s.power == 1 && s.life == 1) == otherSummons)
        else
          assertTrue(ap.battlefield.count(s => s.power == 1 && s.life == 1) == 1)
        assertTrue(testAbilityLevel + " " + otherSummons + " " + bfTestSummon.power, bfTestSummon.power == bfTestSummon.life && bfTestSummon.power == 1+ testAbilityLevel*otherSummons)

        bfTestSummon.restoreOriginalPowerAndLife()
        ap.battlefield.clear()
      }
    }

  }
}
