package server.game

import common.card.Deck
import common.card.ability.change.CardDraw
import common.game.RemoteCard
import server.ClientHandler
import server.game.card.{GameCard, GameSummon}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class Player(val submittedDeck: Deck, val handler: ClientHandler, game: Game) {
  var lifeTotal = 30
  var manaTotal = 0
  var availableMana = 0

  val hand = new Zone
  val battlefield = new Zone
  val pile = new Zone
  val deck = new Zone
  setupDeck()

  def changeLifeBy(value: Int) { lifeTotal += value }

  def refillMana() { availableMana = manaTotal }

  private def setupDeck(){
    for (tuple <- submittedDeck.cards) {
      for(_ <- 0 until tuple._2)
        deck.cards += GameCard.build(tuple._1, this, game.nextCardID)
    }
    shuffleDeck()
  }

  class Zone {
    val cards = new ArrayBuffer[GameCard]()
    def summons: ArrayBuffer[GameSummon] = {
      var summons = new ArrayBuffer[GameSummon]
      cards.foreach((c: GameCard) => { c match {
        case c: GameSummon => summons += c
        case _ =>
      }})
      summons
    }
    def drawRandom = if(cards.size > 0) cards.remove(new Random().nextInt(cards.size)) else null
    def drawTop = if(cards.size > 0) cards.remove(cards.size-1) else null
  }
  def drawCard: CardDraw = {
    if(deck.cards.size > 0){
      hand.cards += deck.drawTop
      val cardDrawn = hand.cards.last
      new CardDraw(handler.getUserName, new RemoteCard(cardDrawn.id, cardDrawn.owner.handler.getUserName, cardDrawn.card), false)
    }
    else{
      lifeTotal -= 4
      var cardIndex = new Random().nextInt(30)
      for(tuple<- submittedDeck.cards){
        if(cardIndex > tuple._2)
          cardIndex -= tuple._2
        else
          return new CardDraw(handler.getUserName, new RemoteCard(game.nextCardID, handler.getUserName, tuple._1), true)
      }
      null
    }
  }

  def shuffleDeck() {
    for(i<-1 until deck.cards.size){
      val positionToSwitch = Random.nextInt(i)
      val tmpCard = deck.cards(positionToSwitch)
      deck.cards(positionToSwitch) = deck.cards(i)
      deck.cards(i) = tmpCard
    }
  }
}