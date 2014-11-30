package common.card.ability

object SpellAbilityLibrary {
  var abilityList: Array[SpellAbility] = new Array[SpellAbility](6)

  private val decreaseSummonStats = new SpellAbility("Infesting ","Infestation","Infestation",2,
    (level: Int) => { "All summons lose " + level + " power and " + level + " life." })

  private val drawCard = new SpellAbility("Researching","Research","Research",3,
    (level: Int) => {"Draw " + level*2 + " cards"})

  private val manaUp = new SpellAbility("Blooming","Bloom","Blooms",2,
    (level: Int) => { "Increase your mana by " + level })

  private val killRandomSummon = new SpellAbility("Deathly","Death","Death",4,
    (level: Int) => { if (level > 1) "Kill " + level + " opponent's random summons." else "Kill 1 opponent's random summon." })

  private val drawCardFromRandomOpponent = new SpellAbility("Stealing","Steal","Stealing",2,
    (level: Int) => { if (level > 1) "Draw " + level + " cards from opponent's decks." else "Draw 1 card from opponent's deck." })

  private val killEachPlayerSummon = new SpellAbility("Sacrificing","Sacrifice","Sacrifice",3,
    (level: Int) => { if (level > 1) "Kill " + level + " random summons of each player." else "Kill 1 random summon of each player." })

  abilityList(0) = decreaseSummonStats
  abilityList(1) = drawCard
  abilityList(2) = manaUp
  abilityList(3) = killRandomSummon
  abilityList(4) = drawCardFromRandomOpponent
  abilityList(5) = killEachPlayerSummon
}