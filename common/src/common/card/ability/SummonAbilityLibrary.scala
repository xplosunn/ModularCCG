package common.card.ability

object SummonAbilityLibrary {

  val abilityList: Array[SummonAbility] = new Array[SummonAbility](6)

  abilityList(0) = new SummonAbility("Digging", "Digger", "Digging", 2, SummonAbility.ON_SUMMON,
      (level: Int) => {
        if( level > 1) "When summoned return " + level + " random cards from your pile to your hand."
        else "When summoned return 1 random card from your pile to your hand."})

  abilityList(1) = new SummonAbility("Grenade", "Grenade", "Grenades", 1, SummonAbility.ON_DEATH,
      (level: Int) => {"On death opponent loses " + level*2 + " life."})

  abilityList(2) = new SummonAbility("Poisoning", "Poisonholder", "Poison", 3, SummonAbility.ON_COMBAT,
    {
      case 1 => "Kills attacking summon after blocking it."
      case 2 => "Kills attacking summon after blocking and it and " + 1 + " other random attacker."
      case level => "Kills attacking summon after blocking and it and " + (level - 1) + " other random attackers."
    })

  abilityList(3) = new SummonAbility("Evading", "Evader", "Evasion", 1, SummonAbility.ON_COMBAT,
    (level: Int) => {"Gets +" + 2*level + " attack and +" + 2*level + " life after battle if not defended."})

  abilityList(4) = new SummonAbility("Warlord", "Warlord", "War", 2, SummonAbility.ON_SUMMON,
    (level: Int) => {"When summoned your other summons get +" + level + " power and +" + level + " life."})

  abilityList(5) = new SummonAbility("Champion", "Champion", "Champions", 2, SummonAbility.ON_SUMMON,
    (level: Int) => {"When summoned gets +" + level + " power and +" + level + " life for each other summon of yours."})
}