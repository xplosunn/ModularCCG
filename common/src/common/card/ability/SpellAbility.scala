package common.card.ability

class SpellAbility(namePrefix: String, name: String, nameSuffix: String, cost: Int, textPerLevel: (Int) => String) extends Ability(namePrefix,name,nameSuffix,cost,textPerLevel) {
  def textForLevel(level: Int) = textPerLevel(level)
}