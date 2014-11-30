package client.window.tab.panel

import java.awt.event.{MouseEvent, MouseListener}
import java.awt.{Dimension, Graphics, Graphics2D}
import javax.swing.{JComponent, JPanel}

import client.window.tab.DuelTab
import common.card.Summon
import common.game.RemoteCard

import scala.swing.Component

class GraphicalRemoteCard(val remoteCard: RemoteCard, duelTab: DuelTab) extends Component{
  private var currentlyAttacking = false

  def attacking_=(b: Boolean){
    if(b != currentlyAttacking){
      currentlyAttacking = b
      repaint()
    }
  }

  def attacking = currentlyAttacking

  var currentPower = -1
  var currentLife = -1
  restorePowerAndLife()

  def restorePowerAndLife(){
    if (remoteCard != null) {
      currentPower = remoteCard.card match {
        case card: Summon => card.power
        case _ => -1
      }
      currentLife = remoteCard.card match {
        case card: Summon => card.life
        case _ => -1
      }
    }
  }

  private val card = new JComponent {
    val cardSize = new Dimension(100,100)
    setPreferredSize(cardSize)
    setMinimumSize(cardSize)
    setMaximumSize(cardSize)

    addMouseListener(new MouseListener {
      override def mouseClicked(e: MouseEvent){if(remoteCard != null) duelTab.cardClicked(GraphicalRemoteCard.this)}

      override def mouseExited(e: MouseEvent){if(remoteCard!=null) duelTab.cardViewer.update(null)}

      override def mouseEntered(e: MouseEvent){if(remoteCard != null) duelTab.cardViewer.update(remoteCard.card)}

      override def mousePressed(e: MouseEvent){}

      override def mouseReleased(e: MouseEvent){}
    })

    override def paintComponent(g: Graphics) {
      g.drawRect(0, 0, 99, 99)
      if (remoteCard == null)
        g.drawString("Card", 40, 40)
      else {
        val text = remoteCard.card.cost + ": " + remoteCard.card.name
        if (g.getFontMetrics.stringWidth(text) <= cardSize.width)
          g.drawString(text, 0, 15)
        else {
          var breakIndex = text.length - 1
          var breakIndex2 = text.length
          while (g.getFontMetrics.stringWidth(text.substring(0, breakIndex)) > cardSize.width || text.charAt(breakIndex) != ' ')
            breakIndex -= 1
          g.drawString(text.substring(0, breakIndex), 0, 15)
          if (breakIndex2 > breakIndex && g.getFontMetrics.stringWidth(text.substring(breakIndex + 1, breakIndex2)) > cardSize.width) {
            while (g.getFontMetrics.stringWidth(text.substring(breakIndex + 1, breakIndex2)) > cardSize.width || text.charAt(breakIndex2) != ' ')
              breakIndex2 -= 1
            g.drawString(text.substring(breakIndex2 + 1, text.length), 0, 45)
          }
          g.drawString(text.substring(breakIndex + 1, breakIndex2), 0, 30)
        }
        remoteCard.card match {
          case card: Summon =>
            g.drawString("" + currentPower, 10, 95)
            g.drawString("" + currentLife, 85, 95)
          case _ =>
        }
      }
    }
  }

  override lazy val peer = new JPanel(){
    override def paintComponent(g: Graphics){
      if (currentlyAttacking) {
        val g2d = g.asInstanceOf[Graphics2D]
        g2d.rotate(-Math.PI / 2, getWidth / 2, getHeight / 2)
        super.paintComponent(g)
      }
      else
        super.paintComponent(g)
    }
  }
  peer.add(card)
}