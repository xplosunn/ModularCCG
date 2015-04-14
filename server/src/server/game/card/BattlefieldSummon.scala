package server.game.card

/**
 * Created by xs on 14-04-2015.
 */
class BattlefieldSummon(val gameSummon: GameSummon) {
  var power: Int = gameSummon.card.power
  var life: Int = gameSummon.card.life

  def restoreOriginalPowerAndLife() {
    power = gameSummon.card.power
    life = gameSummon.card.life
  }

  def changePowerBy(value: Int) {
    power += value
    if (power < 0) power = 0
  }

  def changeLifeBy(value: Int) {
    life += value
    if (life < 0) life = 0
  }

  def setZeroLife() {
    life = 0
  }

  def card = gameSummon.card

  def id = gameSummon.id

  def owner = gameSummon.owner

  def cost = gameSummon.card.cost

}
