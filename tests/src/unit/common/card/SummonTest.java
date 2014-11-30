package unit.common.card;

import common.card.Summon;
import common.card.ability.SummonAbilityLibrary;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by xs on 26-09-2014.
 */
public class SummonTest {
    @Test
    public void cost(){
        Summon summon = new Summon();
        assertEquals(1, summon.cost());
        summon.changePowerBy(2);
        assertEquals(2, summon.cost());
        summon.addAbility(0,1);
        assertEquals(2+SummonAbilityLibrary.abilityList()[0].cost(), summon.cost());
        summon.addAbility(1,2);
        assertEquals(2+SummonAbilityLibrary.abilityList()[0].cost() + 2*SummonAbilityLibrary.abilityList()[1].cost(), summon.cost());
        summon.changeLifeBy(2);
        assertEquals(3+SummonAbilityLibrary.abilityList()[0].cost() + 2*SummonAbilityLibrary.abilityList()[1].cost(), summon.cost());
    }

    @Test
    public void addAbility(){
        Summon summon = new Summon();
        assertFalse(summon.addAbility(-1, 1));
        assertFalse(summon.addAbility(1, -1));
        assertFalse(summon.addAbility(-1, -1));
        assertTrue(summon.addAbility(0, 2));
        assertEquals(2, summon.abilityLevel(0));
        assertTrue(summon.addAbility(1, 1));
        assertEquals(2, summon.abilityLevel(0));
        assertEquals(1, summon.abilityLevel(1));
        assertTrue(summon.addAbility(2, 3));
        assertEquals(2, summon.abilityLevel(0));
        assertEquals(1, summon.abilityLevel(1));
        assertEquals(3, summon.abilityLevel(2));
        assertFalse(summon.addAbility(3,1));
    }

    @Test
    public void name(){
        Summon summon = new Summon();
        assertEquals("Summon", summon.name());
        summon.addAbility(0, 1);
        assertEquals(SummonAbilityLibrary.abilityList()[0].name(), summon.name());
        summon.addAbility(1,1);
        assertEquals(SummonAbilityLibrary.abilityList()[1].namePrefix() + " " + SummonAbilityLibrary.abilityList()[0].name(), summon.name());
        summon.addAbility(2, 1);
        assertEquals(SummonAbilityLibrary.abilityList()[1].namePrefix() + " " + SummonAbilityLibrary.abilityList()[0].name() + " of " + SummonAbilityLibrary.abilityList()[2].nameSuffix(), summon.name());
    }

    @Test
    public void duplicate(){
        Summon summon = new Summon();
        Summon otherSummon = new Summon();
        assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
        assertTrue(CardTest.equalsAndHash(true, summon, otherSummon));
        summon.addAbility(2, 2);
        assertTrue(CardTest.equalsAndHash(false, summon, otherSummon));
        otherSummon.addAbility(2, 2);
        assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
        assertTrue(CardTest.equalsAndHash(true, summon, otherSummon));
        summon.addAbility(1, 1);
        assertTrue(CardTest.equalsAndHash(false, summon, otherSummon));
        otherSummon.addAbility(1, 1);
        assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
        assertTrue(CardTest.equalsAndHash(true, summon, otherSummon));

        for(int i = 0; i < 30; i++){
            summon.changeLifeBy(1);
            assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
            assertTrue(CardTest.equalsAndHash(false, summon, otherSummon));
        }
        for(int i = 0; i < 30; i++){
            summon.changePowerBy(1);
            assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
            assertTrue(CardTest.equalsAndHash(false, summon, otherSummon));
        }
        summon.life(1);
        summon.power(1);
        for(int i = 0; i < 30; i++){
            summon.changePowerBy(1);
            assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
            assertTrue(CardTest.equalsAndHash(false, summon, otherSummon));
        }
        for(int i = 0; i < 30; i++){
            summon.changeLifeBy(1);
            assertTrue(CardTest.equalsAndHash(true, summon, summon.duplicate()));
            assertTrue(CardTest.equalsAndHash(false, summon, otherSummon));
        }
    }
}
