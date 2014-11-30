package client.window.tab

import java.awt.event.{ActionEvent, ActionListener}

import scala.swing._
import BorderPanel.Position._
import common.card.{Summon, Spell, Deck, Card}
import common.card.ability.{Ability, SpellAbilityLibrary, SummonAbilityLibrary}
import client.window.table.{AbilityTableRow, DeckTableRow}
import scala.collection.mutable.ArrayBuffer
import scala.swing.event.ButtonClicked
import client.window.DeckChooser
//TODO: add missing buttons (remove cards from deck)
object ForgeTab extends BoxPanel(Orientation.Horizontal){
  private val deck = new ArrayBuffer[(Card,Int)]()

  private var card: Card = new Summon
  private val cardPanel = new CardPreviewPanel(card)

  val deckTable = new BoxPanel(Orientation.Vertical)

  //Deck Panel
  private val deckLabel = new Label
  private val deckPanel = new BorderPanel(){
    updateDeckLabelText()
    layout(new BoxPanel(Orientation.Vertical){
      contents += new FlowPanel{contents += deckLabel}
    }) = North
    layout(new ScrollPane(new FlowPanel{contents += new ScrollPane(deckTable)})) = Center
    layout(new FlowPanel{
      val newDeck = new Button("New Deck"){
        reactions+={
          case ButtonClicked(b)=>
            deck.remove(0, deck.size)
            deckTable.contents.remove(0, deckTable.contents.length)
            deckTable.repaint()
            updateDeckLabelText()
        }
      }
      val saveDeck = new Button("Save Deck"){
        reactions+={
          case ButtonClicked(b)=>
            DeckChooser.save(this, deck)
        }
      }
      val loadDeck = new Button("Load Deck"){
        reactions+={
          case ButtonClicked(b)=>
            val loadedDeck: Deck = DeckChooser.load(this)
            if(loadedDeck != null){
              deck.remove(0,deck.size)
              deckTable.contents.remove(0, deckTable.contents.length)
              loadedDeck.cards.foreach(tuple =>{
                deck += tuple
                deckTable.contents += new DeckTableRow(tuple._1,tuple._2, cardPanel)
              })
              for(i<-0 until loadedDeck.cards.size){

              }
              deckTable.repaint()
              updateDeckLabelText()
            }
        }
      }
      contents += newDeck
      contents += loadDeck
      contents += saveDeck
    }) = South
  }
  contents += deckPanel

  //Card Preview and Power/Life Panels
  val powerAndLifeChangePanel = new GridPanel(3,1){
    contents += new FlowPanel{
      contents += new Button("+1/+1"){
        reactions += {
          case ButtonClicked(b) =>
            card match {
              case card: Summon =>
                card.changePowerBy(1)
                card.changeLifeBy(1)
                cardPanel.update(card)
            }
        }
      }
      contents += new Label("(+1 cost added)")
    }
    contents += new FlowPanel{
      contents += new Button("+1/-1"){
        reactions += {
          case ButtonClicked(b) =>
            card match {
              case card: Summon =>
                if(card.life > 1){
                  card.changePowerBy(1)
                  card.changeLifeBy(-1)
                  cardPanel.update(card)
                }
            }
        }
      }
    }
    contents += new FlowPanel{
      contents += new Button("-1/+1"){
        reactions += {
          case ButtonClicked(b) =>
            card match {
              case card: Summon =>
                if(card.power > 0){
                  card.changePowerBy(-1)
                  card.changeLifeBy(1)
                  cardPanel.update(card)
                }
            }
        }
      }
    }
  }
  val centerPanel = new BoxPanel(Orientation.Vertical){
    contents += new FlowPanel{ contents += cardPanel }
    contents += powerAndLifeChangePanel
  }
  contents += centerPanel


  //Card Builder Panel
  val abilitiesPanel = new BoxPanel(Orientation.Vertical)

  def setAbilities(abilities: Array[Ability]){
    abilitiesPanel.contents.remove(0,abilitiesPanel.contents.length)
    val margin = new Insets(0,0,0,0)
    for(i<-abilities.indices){
      abilitiesPanel.contents += new FlowPanel(FlowPanel.Alignment.Right)(){
        val abilityRow = new AbilityTableRow(cardPanel,abilities(i))
        contents += abilityRow
        contents += new BoxPanel(Orientation.Vertical){
          contents += new Button("+"){
            peer.setMargin(margin)
            peer.setBounds(0,0,50,50)
            peer.setMaximumSize(new Dimension(50,50))
            peer.addActionListener(new ActionListener(){def actionPerformed(e: ActionEvent){abilityRow.abilityLevelUp()}})
          }
          contents += new Button("-"){
            peer.setMargin(margin)
            peer.setBounds(0,0,50,50)
            peer.setMaximumSize(new Dimension(50,50))
            peer.addActionListener(new ActionListener(){def actionPerformed(e: ActionEvent){abilityRow.abilityLevelDown()}})
          }
        }
        contents += new Button("Add"){
          peer.setBounds(0,0,70,50)
          peer.setMinimumSize(new Dimension(70,50))
          peer.setPreferredSize(new Dimension(70,50))
          peer.addActionListener(new ActionListener(){override def actionPerformed(e: ActionEvent){card.addAbility(i,abilityRow.getLevel);cardPanel.update(card)}})
        }
      }
    }
  }
  setAbilities(SummonAbilityLibrary.abilityList.asInstanceOf[Array[Ability]])

  val buildPanel = new BorderPanel(){
    var topButtons = new FlowPanel(){
      contents += new Button("New Summon"){ reactions += { case ButtonClicked(b) => newSummon()}}
      contents += new Button("New Spell"){ reactions += { case ButtonClicked(b) => newSpell()}}
    }
    layout(topButtons) = North
    layout(new ScrollPane(abilitiesPanel)) = Center

    var bottomButtons = new FlowPanel(){
      var addToDeck = new Button("Add card to deck")
      addToDeck.reactions+={
        case ButtonClicked(b) =>
          var cardAdded = false
          val cardToAdd = card.duplicate
          for(i<-deck.indices)
            if((deck(i)_1).equals(cardToAdd)){
              cardAdded = true
              deck(i) = (deck(i)._1,deck(i)._2+1)
              deckTable.contents(i).asInstanceOf[DeckTableRow].quantity+=1
            }
          if(!cardAdded){
            deck += ((cardToAdd,1))
            deckTable.contents += new DeckTableRow(deck.last._1, deck.last._2, cardPanel)
          }
          deckTable.repaint()
          updateDeckLabelText()
      }
      contents+=addToDeck
    }
    layout(bottomButtons) = South
    }
  contents += buildPanel


  def updateDeckLabelText() {
    var sum: Int = 0
    for(i<-deck.indices)
      sum += (deck(i)_2)
    deckLabel.text = "Deck " + sum + "/30"
  }

  def newSummon() {
    card = new Summon
    powerAndLifeChangePanel.visible = true
    powerAndLifeChangePanel.repaint()
    cardPanel.update(card)
    setAbilities(SummonAbilityLibrary.abilityList.asInstanceOf[Array[Ability]])
  }

  def newSpell() {
    card = new Spell
    powerAndLifeChangePanel.visible = false
    powerAndLifeChangePanel.repaint()
    cardPanel.update(card)
    setAbilities(SpellAbilityLibrary.abilityList.asInstanceOf[Array[Ability]])
  }
}