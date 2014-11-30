package common.card

import java.io.Serializable

import common.card.ability.{SpellAbilityLibrary, SummonAbilityLibrary}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Deck extends Serializable {

  val cards = mutable.HashMap[Card,Int]()

  def add(card: Card, quantity: Int) =
      cards put (card,quantity)

  def validate: Boolean = {
    if (cards.size == 0)
      return false
    var sum: Int = 0
    cards.foreach(tuple => {
      if(!tuple._1.validate)
        return false

      for(i <- 0 until 3){
        if(tuple._1.abilityLevel(i) < -1)
          return false
        if((tuple._1.abilityLevel(i) == -1) != (tuple._1.abilityLibraryIndex(i) == -1))
          return false
      }
      if(tuple._1 == null || tuple._2 > 30 || tuple._2 <= 0)
        return false
      sum += tuple._2
    })
    sum == 30
  }
}