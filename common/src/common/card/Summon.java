package common.card;

import common.card.ability.SummonAbilityLibrary;
import opensource.apache.HashCodeBuilder;

/**
 * Created by xs on 26-09-2014.
 */
public class Summon extends Card {
    private int power = 1;
    private int life = 1;
    // Array that couples the index for each ability in the SpellAbilityLibrary
    // with it's level in this card
    protected final int[][] abilityArray = new int[MAXIMUM_ABILITIES()][2];

    public Summon(){
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

    public int power(){
        return power;
    }

    public int life(){
        return life;
    }

    public void power(int newValue){
        power = newValue;
    }

    public void life(int newValue){
        life = newValue;
    }

    public void changePowerBy(int value) {
        power += value;
        if (power < 0) power = 0;
    }

    public void changeLifeBy(int value) {
        life += value;
        if (life < 1) life = 1;
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
        Summon dup = new Summon();
        for (int i = 0; i < abilityArray.length; i++) {
            if(abilityArray[i][0] >= 0){
                dup.addAbility(abilityArray[i][0], abilityArray[i][1]);
            }
        }
        dup.power(power);
        dup.life(life);
        return dup;
    }

    @Override
    public String name() {
        if(abilityArray[0][0] < 0)
            return "Summon";
        else{
            String name = SummonAbilityLibrary.abilityList()[abilityArray[0][0]].name();
            if(abilityArray[1][0] >= 0)
                name = SummonAbilityLibrary.abilityList()[abilityArray[1][0]].namePrefix() + " " + name;
            if(abilityArray[2][0] >= 0)
                name = name + " of " + SummonAbilityLibrary.abilityList()[abilityArray[2][0]].nameSuffix();
            return name;
        }
    }

    @Override
    public int cost() {
        int costTotal = (power + life)/2;
        for (int i = 0; i < abilityArray.length; i++) {
            if(abilityArray[i][0] >= 0)
                costTotal += SummonAbilityLibrary.abilityList()[abilityArray[i][0]].cost() * abilityArray[i][1];
        }
        return costTotal;
    }

    @Override
    public String abilityText(int abilityArrayIndex) {
        if(abilityArrayIndex < abilityArray.length && abilityArray[abilityArrayIndex][0] >= 0)
            return SummonAbilityLibrary.abilityList()[abilityArray[abilityArrayIndex][0]].textForLevel(abilityArray[abilityArrayIndex][1]);
        else
            return "";
    }

    @Override
    public int abilityLevel(int abilityArrayIndex) {
        if(abilityArrayIndex >= 0 && abilityArrayIndex < abilityArray.length && abilityArray[abilityArrayIndex][0] >= 0)
            return abilityArray[abilityArrayIndex][1];
        else
            return -1;
    }

    @Override
    public int abilityLibraryIndex(int abilityArrayIndex) {
        if(abilityArrayIndex >= 0 && abilityArrayIndex < abilityArray.length && abilityArray[abilityArrayIndex][0] >= 0)
            return abilityArray[abilityArrayIndex][0];
        else
            return -1;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 29). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(abilityArray).
                append(power).
                append(life).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Summon){
            Summon c = (Summon) obj;
            if(!name().equals(c.name()))
                return false;
            if(power != c.power())
                return false;
            if(life != c.life())
                return false;
            for (int i = 0; i < MAXIMUM_ABILITIES(); i++)
                if (abilityLevel(i) != c.abilityLevel(i) || abilityLibraryIndex(i) != c.abilityLibraryIndex(i))
                    return false;
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean validate() {
        if(power < 0 || life < 1)
            return false;
        for (int i = 0; i < MAXIMUM_ABILITIES(); i++) {
            if (((abilityArray[i][0] == -1) != (abilityArray[i][1] == -1))
                    || abilityArray[i][0] < -1 || abilityArray[i][1] < -1
                    || abilityArray[i][1] > SummonAbilityLibrary.abilityList().length)
                return false;
        }
        return true;
    }
}
