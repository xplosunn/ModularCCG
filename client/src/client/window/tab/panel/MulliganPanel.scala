package client.window.tab.panel

import java.awt.Color
import java.awt.event.{MouseEvent, MouseListener}
import javax.swing.BorderFactory

import client.ClientSession
import client.window.tab.CardPreviewPanel
import common.game.RemoteCard

import scala.collection.mutable.ArrayBuffer
import scala.swing._
import scala.swing.event.ButtonClicked

class MulliganPanel(gameID: Int, cardPreviewPanel: CardPreviewPanel) extends BoxPanel(Orientation.Horizontal){
  val cardsContainer = new FlowPanel()
  val cardLabels = new ArrayBuffer[CardRep]()
  val submitButton = new Button("Submit"){
    reactions += {
      case ButtonClicked(b) =>
        if(cardLabels.count(l => l.isSelected) == 3)
          ClientSession.SessionVars.serverHandler.Duel.selectHand(gameID, selectedCardIDs)
    }
  }
  contents += new BoxPanel(Orientation.Vertical){
    contents += cardsContainer
    contents += submitButton
  }


  private def selectedCardIDs: Array[Int] = {
    var ids = new Array[Int](0)
    cardLabels.foreach(label => if(label.isSelected) ids ++= Array(label.remoteCard.id))
    ids
  }

  def addCards(remoteCards: Array[RemoteCard]) {
    remoteCards.foreach(remoteCard => {
      val cardRep = new CardRep(remoteCard)
      cardLabels += cardRep
      cardsContainer.contents += cardRep.getLabel
    })
    cardsContainer.repaint()
  }
  
  def clear(){
    cardsContainer.contents.clear()
    cardsContainer.repaint()
    visible = false
    repaint()
  }

  class CardRep(val remoteCard: RemoteCard){
    private val label = new Label(remoteCard.card.cost() + ": " + remoteCard.card.name()) {
      val blackBorder = BorderFactory.createLineBorder(Color.BLACK)
      val redBorder = BorderFactory.createLineBorder(Color.RED)

      def isSelected: Boolean = border == redBorder
      def select() {border = redBorder; repaint()}
      def unselect() {border = blackBorder; repaint()}

      peer.setBorder(blackBorder)
      peer.addMouseListener(new MouseListener {
        override def mouseExited(e: MouseEvent) {cardPreviewPanel.update(null)}

        override def mouseClicked(e: MouseEvent) {
          if (!isSelected) {
            if (cardLabels.count(label => label.isSelected) < 3){
              select()
            }
          } else
            unselect()
        }

        override def mouseEntered(e: MouseEvent) {
          cardPreviewPanel.update(remoteCard.card)
        }

        override def mousePressed(e: MouseEvent) {}

        override def mouseReleased(e: MouseEvent) {}
      })
    }

    def isSelected: Boolean = label.isSelected

    def getLabel = label

  }


}
