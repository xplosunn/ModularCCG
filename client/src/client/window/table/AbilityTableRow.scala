package client.window.table

import java.awt.{Dimension, Graphics}
import javax.swing.JComponent

import client.window.tab.CardPreviewPanel
import common.card.ability.Ability

import scala.swing.Component

class AbilityTableRow(cardPanel: CardPreviewPanel, ability: Ability) extends Component {
  private var abilityLevel = 1

  def getLevel: Int = abilityLevel

  def abilityLevelUp(){
    if(abilityLevel < 10){
      abilityLevel += 1
      repaint()
    }
  }

  def abilityLevelDown(){
    if(abilityLevel > 1){
      abilityLevel -= 1
      repaint()
    } 
  }

  override lazy val peer = new JComponent{
    val numberZoneWidth = 20
    val textOffset = 5
    setMinimumSize(new Dimension(400, 50))
    setPreferredSize(new Dimension(400, 50))
    override def paintComponent(g: Graphics){
      g.drawLine(getWidth-numberZoneWidth,0, getWidth-numberZoneWidth, 49)
      g.drawLine(0, 49, getWidth, 49)

      val text = ability textPerLevel abilityLevel
      g.drawString("" + ability.cost * abilityLevel, getWidth-numberZoneWidth+textOffset, 30)

      if (g.getFontMetrics.stringWidth(text) <= getWidth-numberZoneWidth-textOffset)
        g.drawString(text, textOffset, 15)
      else {
        var breakIndex: Int = text.length-1
        while(g.getFontMetrics.stringWidth(text.substring(0, breakIndex))> getWidth-numberZoneWidth-textOffset){
          breakIndex-=1
          while(text.charAt(breakIndex) != ' ')
            breakIndex-=1
        }
        g.drawString(text.substring(0, breakIndex), textOffset, 15)
        g.drawString(text.substring(breakIndex+1), textOffset, 35)
      }
    }
  }
}