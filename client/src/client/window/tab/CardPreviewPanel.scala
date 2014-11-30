package client.window.tab

import scala.swing.{TextArea, BorderPanel, GridPanel, Label}
import scala.swing.BorderPanel.Position._

import common.card.{Summon, Card}
import javax.swing.{BorderFactory, ImageIcon}
import java.awt.Dimension
import javax.swing.border.BevelBorder

class CardPreviewPanel(var card: Card) extends BorderPanel {
  border = BorderFactory.createCompoundBorder(new BevelBorder(BevelBorder.RAISED),new BevelBorder(BevelBorder.LOWERED))
  val sizeDim = new Dimension(250,300)
  minimumSize = sizeDim
  maximumSize = sizeDim
  peer.setMinimumSize(sizeDim)
  peer.setMaximumSize(sizeDim)
  peer.setPreferredSize(sizeDim)

  var topText = new Label()
  var abilitiesText = ""
  val abilityTextArea = new TextArea(abilitiesText)
  abilityTextArea.preferredSize = new Dimension(250,0)
  abilityTextArea.rows = 6
  abilityTextArea.wordWrap = true
  abilityTextArea.lineWrap = true
  abilityTextArea.editable = false
  val powerLabel = new Label()
  val lifeLabel = new Label()

  def update(card: Card) {
    if(card == null){
      topText.text = ""
      abilitiesText = ""
      abilityTextArea.text = ""
      powerLabel.text = ""
      lifeLabel.text = ""
    }else{
      topText.text_=(card.cost + ": " + card.name)
      abilitiesText = ""
      abilitiesText += card.abilityText(0)
      for(i<-1 to card.MAXIMUM_ABILITIES-1)
        abilitiesText += System.lineSeparator()+card.abilityText(i)
      abilityTextArea.text = abilitiesText

      powerLabel.text = card match {
        case card: Summon => "" + card.power
        case _ => ""
      }
      lifeLabel.text = card match {
        case card: Summon => "" + card.life
        case _ => ""
      }
    }
    repaint()
  }
  update(card)

  //Cost & Name
  layout(topText) = North

  //Image & Text
  layout(new BorderPanel() {
    //Image
    layout(new Label { icon = new ImageIcon("client/resources/blankcard.gif") }) = North
    //Text
    layout(abilityTextArea) = Center

  }) = Center

  //Power & Life
  var powerAndLifePanel = new GridPanel(1, 2) {
    contents += powerLabel
    contents += lifeLabel
  }
  layout(powerAndLifePanel) = South
}