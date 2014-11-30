package client.window

import java.awt.event.{ComponentEvent, ComponentListener}

import client.ClientSession

import scala.swing.Frame
import scala.swing.TabbedPane

import client.window.tab.{DuelTab, PlayTab, ForgeTab, LobbyTab}
import java.awt.Dimension
import scala.swing.TabbedPane._

class MainWindow{

  val frame = new Frame() {
    override def closeOperation(){
      System.exit(0)
    }
    minimumSize = new Dimension(750,650)
    title = ClientSession.Constants.GameName
    contents = new TabbedPane() {

      pages += new Page("Lobby", LobbyTab)
      pages += new Page("Forge", ForgeTab)
      pages += new Page("Play", PlayTab)
    }
    /*peer.addComponentListener(new ComponentListener {
      override def componentShown(e: ComponentEvent){}

      override def componentHidden(e: ComponentEvent){}

      override def componentMoved(e: ComponentEvent){}

      override def componentResized(e: ComponentEvent){
        repaint()
      }
    })*/
  }

  def show() {frame.visible = true}
  def dispose() = frame.dispose()

  def addGameTab(id: Int, panel: DuelTab){
    frame.contents(0).asInstanceOf[TabbedPane].pages += new Page("Match #" + id,panel)
  }
  
  def removeGameTab(panel: DuelTab){
    val pages = frame.contents(0).asInstanceOf[TabbedPane].pages
    for(page<-pages)
      if(page.content == panel){
        pages -= page
        return
      }
  }
}