package unit.common.card;

import common.card.Card;
import common.card.Spell;
import common.card.ability.SpellAbilityLibrary;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by xs on 26-09-2014.
 */
public class SpellTest {
    @Test
    public void cost(){
        Spell spell = new Spell();
        assertEquals(0, spell.cost());
        spell.addAbility(0,1);
        assertEquals(SpellAbilityLibrary.abilityList()[0].cost(), spell.cost());
        spell.addAbility(1,2);
        assertEquals(SpellAbilityLibrary.abilityList()[0].cost() + 2*SpellAbilityLibrary.abilityList()[1].cost(), spell.cost());
    }

    @Test
    public void addAbility(){
        Spell spell = new Spell();
        assertFalse(spell.addAbility(-1, 1));
        assertFalse(spell.addAbility(1, -1));
        assertFalse(spell.addAbility(-1, -1));
        assertTrue(spell.addAbility(0, 2));
        assertEquals(2, spell.abilityLevel(0));
        assertTrue(spell.addAbility(1, 1));
        assertEquals(2, spell.abilityLevel(0));
        assertEquals(1, spell.abilityLevel(1));
        assertTrue(spell.addAbility(2, 3));
        assertEquals(2, spell.abilityLevel(0));
        assertEquals(1, spell.abilityLevel(1));
        assertEquals(3, spell.abilityLevel(2));
        assertFalse(spell.addAbility(3,1));
    }

    @Test
    public void name(){
        Spell spell = new Spell();
        assertEquals("Spell", spell.name());
        spell.addAbility(0,1);
        assertEquals(SpellAbilityLibrary.abilityList()[0].name(), spell.name());
        spell.addAbility(1,1);
        assertEquals(SpellAbilityLibrary.abilityList()[1].namePrefix() + " " + SpellAbilityLibrary.abilityList()[0].name(), spell.name());
        spell.addAbility(2,1);
        assertEquals(SpellAbilityLibrary.abilityList()[1].namePrefix() + " " + SpellAbilityLibrary.abilityList()[0].name() + " of " + SpellAbilityLibrary.abilityList()[2].nameSuffix(), spell.name());
    }

    @Test
    public void duplicate(){
        Spell spell = new Spell();
        Spell sameSpell = new Spell();
        assertTrue(CardTest.equalsAndHash(true, spell, spell.duplicate()));
        assertTrue(CardTest.equalsAndHash(true, spell, sameSpell));
        spell.addAbility(2,2);
        assertTrue(CardTest.equalsAndHash(false, spell, sameSpell));
        sameSpell.addAbility(2,2);
        assertTrue(CardTest.equalsAndHash(true, spell, spell.duplicate()));
        assertTrue(CardTest.equalsAndHash(true, spell, sameSpell));
        spell.addAbility(1,1);
        assertTrue(CardTest.equalsAndHash(false, spell, sameSpell));
        sameSpell.addAbility(1,1);
        assertTrue(CardTest.equalsAndHash(true, spell, spell.duplicate()));
        assertTrue(CardTest.equalsAndHash(true, spell, sameSpell));
    }




}
