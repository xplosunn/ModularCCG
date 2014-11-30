package unit.common.game;

import common.card.Spell;
import common.game.RemoteCard;
import org.junit.Test;

/**
 * Created by xs on 28-09-2014.
 */
public class RemoteCardTest {

    @Test (expected = IllegalArgumentException.class)
    public void instantiateNull1(){
        new RemoteCard(1, null, new Spell());
    }

    @Test (expected = IllegalArgumentException.class)
    public void instantiateNull2(){
        new RemoteCard(1, "", new Spell());
    }

    @Test (expected = IllegalArgumentException.class)
    public void instantiateNull3(){
        new RemoteCard(1, "fakePlayer", null);
    }
}
