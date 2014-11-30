package common.card.ability

abstract class Ability(val namePrefix: String, val name: String, val nameSuffix: String, val cost: Int, val textPerLevel: (Int) => String) extends Serializable {

}