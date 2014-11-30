package common.card;

import java.io.Serializable;

public abstract class Card implements Serializable{
    public abstract boolean addAbility(int abilityLibraryIndex, int level);
    public abstract void removeAbility(int abilityArrayIndex);
    public abstract Card duplicate();

    public abstract int MAXIMUM_ABILITIES();
    public abstract String name();
    public abstract int cost();
    public abstract String abilityText(int abilityArrayIndex);
    public abstract int abilityLevel(int abilityArrayIndex);
    public abstract int abilityLibraryIndex(int abilityArrayIndex);

    public abstract boolean validate();
}
