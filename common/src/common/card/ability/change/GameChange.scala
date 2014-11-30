package common.card.ability.change

object GameChange{
  object Value {
    val LIFE = 0
    val POWER = 1
    val MANA = 2
  }

  object Zone {
    val HAND = 0
    val PILE = 1
    val BATTLEFIELD = 2
    val DECK = 3
  }
}

abstract class GameChange extends Serializable{

}
