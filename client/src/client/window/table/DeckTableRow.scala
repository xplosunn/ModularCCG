package client.window.table

import scala.swing.Component
import javax.swing.JComponent
import java.awt.{Dimension, Graphics}
import common.card.Card
import java.awt.event.{MouseEvent, MouseListener}
import client.window.tab.CardPreviewPanel

class DeckTableRow(val card: Card, var quantity: Int, cardPreview: CardPreviewPanel) extends Component{
  override lazy val peer = new JComponent(){
    private val WIDTH = 280
    private val HEIGHT = 25
    override def getWidth = {
      if(WIDTH+1 > getParent.getWidth)
        WIDTH +1
      else
        getParent.getWidth
    }
    override def getHeight = HEIGHT+1
    setPreferredSize(new Dimension(WIDTH, HEIGHT))

    override def paintComponent(g: Graphics){
      g.drawString(card.name,5,HEIGHT-5)
      g.drawString(quantity + "", getWidth()-20, HEIGHT-5)
      g.drawLine(getWidth()-30,0,getWidth()-30,HEIGHT)
      g.drawLine(0, HEIGHT, getWidth(), HEIGHT)
    }
  }
  peer.addMouseListener(new MouseListener {
    var previousCard: Card = null
    override def mouseClicked(e: MouseEvent){}

    override def mouseExited(e: MouseEvent){
      cardPreview.update(previousCard)
    }

    override def mouseEntered(e: MouseEvent){
      previousCard = cardPreview.card
      cardPreview.update(card)
    }

    override def mousePressed(e: MouseEvent){}

    override def mouseReleased(e: MouseEvent){}
  })
}