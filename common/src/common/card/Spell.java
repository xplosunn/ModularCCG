package common.card;

import common.card.ability.SpellAbilityLibrary;
import opensource.apache.HashCodeBuilder;

/**
 * Created by xs on 26-09-2014.
 */
public class Spell extends Card{
    // Array that couples the index for each ability in the SpellAbilityLibrary
    // with it's level in this card
    private final int[][] abilityArray = new int[MAXIMUM_ABILITIES()][2];

    public Spell(){
        for (int i = 0; i < abilityArray.length; i++) {
            for (int j = 0; j < abilityArray[i].length; j++) {
                abilityArray[i][j] = -1;
            }
        }
    }

    @Override
    public int MAXIMUM_ABILITIES() {
        return 3;
    }

    @Override
    public boolean addAbility(int abilityLibraryIndex, int level) {
        if(abilityLibraryIndex < 0 || level <= 0)
            return false;
        for(int i = 0; i < abilityArray.length; i++){
            if(abilityArray[i][0] == abilityLibraryIndex){
                abilityArray[i][1] = level;
                return true;
            }
            else if(abilityArray[i][0] < 0){
                abilityArray[i][0] = abilityLibraryIndex;
                abilityArray[i][1] = level;
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeAbility(int abilityArrayIndex) {
        if(abilityArrayIndex >= abilityArray.length)
            return;
        for (int i = abilityArrayIndex; i < abilityArray.length-2; i++) {
            abilityArray[i][0] = abilityArray[i+1][0];
            abilityArray[i][1] = abilityArray[i+1][1];
        }
        abilityArray[abilityArray.length-1][0] = -1;
        abilityArray[abilityArray.length-1][1] = -1;
    }

    @Override
    public Card duplicate() {
        Spell dup = new Spell();
        for (int i = 0; i < abilityArray.length; i++) {
            if(abilityArray[i][0] >= 0){
                dup.addAbility(abilityArray[i][0], abilityArray[i][1]);
            }
        }
        return dup;
    }

    @Override
    public String name() {
        if(abilityArray[0][0] < 0)
            return "Spell";
        else{
            String name = SpellAbilityLibrary.abilityList()[abilityArray[0][0]].name();
            if(abilityArray[1][0] >= 0)
                name = SpellAbilityLibrary.abilityList()[abilityArray[1][0]].namePrefix() + " " + name;
            if(abilityArray[2][0] >= 0)
                name = name + " of " + SpellAbilityLibrary.abilityList()[abilityArray[2][0]].nameSuffix();
            return name;
        }
    }

    @Override
    public int cost() {
        int costTotal = 0;
        for (int i = 0; i < abilityArray.length; i++) {
            if(abilityArray[i][0] >= 0)
                costTotal += SpellAbilityLibrary.abilityList()[abilityArray[i][0]].cost() * abilityArray[i][1];
        }
        return costTotal;
    }

    @Override
    public String abilityText(int abilityArrayIndex) {
        if(abilityArrayIndex < abilityArray.length && abilityArray[abilityArrayIndex][0] >= 0)
            return SpellAbilityLibrary.abilityList()[abilityArray[abilityArrayIndex][0]].textForLevel(abilityArray[abilityArrayIndex][1]);
        else
            return "";
    }

    @Override
    public int abilityLevel(int abilityArrayIndex) {
        if(abilityArrayIndex < abilityArray.length && abilityArray[abilityArrayIndex][0] >= 0)
            return abilityArray[abilityArrayIndex][1];
        else
            return -1;
    }

    @Override
    public int abilityLibraryIndex(int abilityArrayIndex) {
        if(abilityArrayIndex < abilityArray.length && abilityArray[abilityArrayIndex][0] >= 0)
            return abilityArray[abilityArrayIndex][0];
        else
            return -1;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 29). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(abilityArray).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Spell){
            Spell c = (Spell) obj;
            if(!name().equals(c.name()))
                return false;
            for(int i = 0; i < MAXIMUM_ABILITIES(); i++)
                if(abilityLevel(i) != c.abilityLevel(i) || abilityLibraryIndex(i) != c.abilityLibraryIndex(i))
                    return false;
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean validate() {
        if(abilityArray[0][0] < 0)
            return false;
        for (int i = 0; i < MAXIMUM_ABILITIES(); i++) {
            if (((abilityArray[i][0] == -1) != (abilityArray[i][1] == -1))
                    || abilityArray[i][0] < -1 || abilityArray[i][1] < -1
                    || abilityArray[i][1] > SpellAbilityLibrary.abilityList().length)
                return false;
        }
        return true;
    }
}
