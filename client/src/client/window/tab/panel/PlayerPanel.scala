package client.window.tab.panel

import java.awt.Dimension
import java.awt.event.{MouseEvent, MouseListener}
import javax.swing.BorderFactory

import client.window.tab.CardPreviewPanel
import client.window.tab.panel.GraphicalRemoteCard
import common.card.Card

import scala.collection.mutable.ArrayBuffer
import scala.swing.BorderPanel.Position._
import scala.swing.event.ButtonClicked
import scala.swing._

/**
 * The panel for each player within a common.game. Holds any known game information related to him/her.
 */
object PlayerPanel{
  object Positions{
    val Top: Int = 0
    val Bottom: Int = 1
  }
}
class PlayerPanel(val player: String, val position: Int, val cardViewer: CardPreviewPanel, stepPanel: StepPanel){
  val handPanel = new FlowPanel(){
    def addCard(card: GraphicalRemoteCard){
      contents += card
      repaint()
    }
    def removeCard(cardID: Int): GraphicalRemoteCard = {
      contents.foreach {
        case c: GraphicalRemoteCard =>
          if (c.remoteCard.id == cardID) {
            contents -= c
            repaint()
            return c
          }
        case _ =>
      }
      null
    }
  }
  val battleFieldPanel = new FlowPanel(){
    def cards = {
      val summons = new ArrayBuffer[GraphicalRemoteCard]
      contents.foreach { case c: GraphicalRemoteCard => summons += c }
      summons
    }
  }
  val playerInfoPanel = new GridPanel(5, 1){
    border = BorderFactory.createTitledBorder(player)

    val pile = new ArrayBuffer[GraphicalRemoteCard]()

    private var life = 30
    val lifeLabel = new Label("Life: " + life)
    private var manaTotal = 0
    private var manaLeft = 0
    val manaLabel = new Label("Mana: " + manaLeft + "/" + manaTotal)
    private var deckSize = 30
    val deckSizeLabel = new Label("Cards in deck: " + deckSize)
    val pileButton = new Button("Cards in pile: " + pile.size){
      private var pileWindow: PileWindow = null
      reactions += {
        case ButtonClicked(b) =>
          if(pileWindow != null)
            pileWindow.dispose()
          pileWindow = new PileWindow(pile)
          pileWindow.show()
      }

      def updatePileWindow(){ if(pileWindow != null) pileWindow.update()}

      private class PileWindow(val pile: ArrayBuffer[GraphicalRemoteCard]) {
        private val frame = new Frame {
          title = player + "'s pile"
          populate()
          preferredSize = new Dimension(200,200)
          size = preferredSize
          def populate() {
            contents = new ScrollPane {
              contents = new BoxPanel(Orientation.Vertical) {
                pile.foreach(graphicalRemoteCard => {
                  if (graphicalRemoteCard == null)
                    contents += new Button("unknown graphical card") {
                      enabled = false
                    }
                  else if (graphicalRemoteCard.remoteCard == null)
                    contents += new Button("unknown remote card") {
                      enabled = false
                    }
                  else if (graphicalRemoteCard.remoteCard.card == null)
                    contents += new Button("unknown card") {
                      enabled = false
                    }
                  else
                    contents += new Button(graphicalRemoteCard.remoteCard.card.name) {
                      enabled = false
                      peer.addMouseListener(new MouseListener {
                        val card = graphicalRemoteCard.remoteCard.card
                        var previousCard: Card = null
                        override def mouseExited(e: MouseEvent){
                          cardViewer.update(previousCard)
                        }

                        override def mouseClicked(e: MouseEvent){}

                        override def mouseEntered(e: MouseEvent){
                          previousCard = cardViewer.card
                          cardViewer.update(card)
                        }

                        override def mousePressed(e: MouseEvent){}

                        override def mouseReleased(e: MouseEvent){}
                      })
                    }
                })
              }
            }
          }
        }

        def show() = frame.visible = true

        def update() {
          frame.populate()
          frame.repaint()
        }

        def dispose() = frame.dispose()
      }
    }
    contents += lifeLabel
    contents += manaLabel
    contents += deckSizeLabel
    contents += pileButton

    def setLife(value: Int){ life = value; lifeLabel.text = "Life: " + value; lifeLabel.repaint()}
    def getLife = life
    def setManaTotal(value: Int){ manaTotal = value; manaLabel.text = "Mana: " + manaLeft + "/" + manaTotal; manaLabel.repaint()}
    def getManaTotal = manaTotal
    def setManaLeft(value: Int){ manaLeft = value; manaLabel.text = "Mana: " + manaLeft + "/" + manaTotal; manaLabel.repaint()}
    def getManaLeft = manaLeft
    def setDeckSize(value: Int){ deckSize = value; deckSizeLabel.text = "Cards in deck: " + value; deckSizeLabel.repaint()}
    def getDeckSize = deckSize
    def updatePile(){ pileButton.text = "Cards in pile: " + pile.size; pileButton.repaint(); pileButton.updatePileWindow()}
  }

  val panel = new BorderPanel{
    position match{
      case PlayerPanel.Positions.Top => layout(new ScrollPane(handPanel)) = North
      case PlayerPanel.Positions.Bottom => layout(new ScrollPane(handPanel)) = South
    }
    if(stepPanel != null){
      layout(new BorderPanel{
        layout(stepPanel) = South
        layout(battleFieldPanel) = Center
      }) = Center
    }
    else
      layout(battleFieldPanel) = Center
    layout(playerInfoPanel) = West
  }
}
