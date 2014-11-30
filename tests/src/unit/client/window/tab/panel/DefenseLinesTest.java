package unit.client.window.tab.panel;


import client.window.tab.panel.DefenseLines;
import client.window.tab.panel.GraphicalRemoteCard;
import common.card.Summon;
import common.game.RemoteCard;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefenseLinesTest {
    @Test
    public void addAndRemoveDefenders(){
        GraphicalRemoteCard attacker = new GraphicalRemoteCard(new RemoteCard(0, "attackingPlayer", new Summon()),null);
        GraphicalRemoteCard defender1 = new GraphicalRemoteCard(new RemoteCard(1, "defendingPlayer", new Summon()),null);
        GraphicalRemoteCard defender2 = new GraphicalRemoteCard(new RemoteCard(2, "defendingPlayer", new Summon()),null);
        GraphicalRemoteCard defender3 = new GraphicalRemoteCard(new RemoteCard(3, "defendingPlayer", new Summon()),null);
        attacker.attacking_$eq(true);
        DefenseLines defenseLines = new DefenseLines();
        assertFalse(defenseLines.isDefender(null));
        assertFalse(defenseLines.isDefender(defender1));
        defenseLines.add(attacker, defender1);
        assertTrue(defenseLines.isDefender(defender1));
        defenseLines.add(attacker, defender2);
        assertTrue(defenseLines.isDefender(defender2));
        defenseLines.add(attacker, defender3);
        assertTrue(defenseLines.isDefender(defender3));
        defenseLines.removeDefender(defender2);
        assertTrue(defenseLines.isDefender(defender1));
        assertFalse(defenseLines.isDefender(defender2));
        assertTrue(defenseLines.isDefender(defender3));
        defenseLines.clear();
        assertFalse(defenseLines.isDefender(defender1));
        assertFalse(defenseLines.isDefender(defender2));
        assertFalse(defenseLines.isDefender(defender3));
    }
}
