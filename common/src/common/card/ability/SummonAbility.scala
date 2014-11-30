package common.card.ability

object SummonAbility{
  //Timing
  val ON_SUMMON = 0
  val ON_DEATH = 1
  val ON_COMBAT = 2

}
class SummonAbility(namePrefix: String, name: String, nameSuffix: String, cost: Int, val timing: Int, textPerLevel:(Int)=>String) extends Ability(namePrefix,name,nameSuffix,cost,textPerLevel){
  def textForLevel(level: Int) = textPerLevel(level)
}