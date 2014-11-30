package unit.common.card

import common.card.{Deck, Summon}
import org.junit.Assert._
import org.junit.Test

class DeckTest {
  @Test def validate() {
    assertFalse(new Deck().validate)

    assertFalse(new Deck{
      add(new Summon, 5)
      add(new Summon, 25)
    }.validate)

    assertFalse(new Deck{
      add(new Summon, -1)
      add(new Summon{power(3)}, 30)
    }.validate)

    assertFalse(new Deck{
      add(new Summon, -1)
      add(new Summon{power(3)}, 31)
    }.validate)

    assertFalse(new Deck{
      add(new Summon, 0)
      add(new Summon{power(3)}, 30)
    }.validate)

    assertFalse(new Deck{
      add(new Summon{life(2)}, 29)
      add(new Summon, 2)
    }.validate)

    assertFalse(new Deck{
      add(new Summon{
        abilityArray(0)(0) = 0
        abilityArray(0)(1) = -1
        }, 30)
    }.validate)

    assertFalse(new Deck{
      add(new Summon{
        abilityArray(0)(0) = -1
        abilityArray(0)(1) = 0
      }, 30)
    }.validate)

    assertTrue(new Deck{
      add(new Summon, 30)
    }.validate)
  }
}