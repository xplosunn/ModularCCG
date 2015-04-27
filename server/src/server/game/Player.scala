package server.game

import common.card.Deck
import common.card.ability.change.CardDraw
import common.card.ability.change.GameChange._
import common.game.RemoteCard
import server.ClientHandler
import server.game.card.{BattlefieldSummon, GameCard, GameSummon}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class Player(val submittedDeck: Deck, val handler: ClientHandler, game: Game) {
  var lifeTotal = 30
  var manaTotal = 0
  var availableMana = 0

  val hand = new ArrayBuffer[GameCard]()
  val battlefield = new ArrayBuffer[BattlefieldSummon]()
  val pile = new ArrayBuffer[GameCard]()
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

    def drawRandom: GameCard = if(cards.size > 0) cards.remove(new Random().nextInt(cards.size)) else null
    def drawTop: GameCard = if(cards.size > 0) cards.remove(cards.size-1) else null
  }

  def drawCard: CardDraw = {
    if(deck.cards.size > 0){
      hand += deck.drawTop
      val cardDrawn = hand.last
      new CardDraw(handler.userName, cardDrawn.remoteCard, false)
    }
    else{
      lifeTotal -= 4
      var cardIndex = new Random().nextInt(30)
      for(tuple<- submittedDeck.cards){
        if(cardIndex > tuple._2)
          cardIndex -= tuple._2
        else
          return new CardDraw(handler.userName, new RemoteCard(game.nextCardID, handler.userName, tuple._1), true)
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