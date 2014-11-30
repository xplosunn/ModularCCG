package client.window.tab

import client.ClientSession
import client.window.{DeckChooser, MainWindow}

import scala.swing.event.ButtonClicked
import scala.swing.{ComboBox, FlowPanel, Button, BorderPanel}
import BorderPanel.Position._

object PlayTab extends BorderPanel{
    val comboBoxOptions = Array("One on One Duel")
    val comboBox = new ComboBox[String](comboBoxOptions)
    val playButton = new Button("Queue for game"){
      reactions += {
        case ButtonClicked(b) =>
          val deck = DeckChooser.load(this)
          val serverHandler = ClientSession.SessionVars.serverHandler
          if (deck != null && serverHandler != null)
            serverHandler.Duel.queue(deck)
      }
  }
  layout(new FlowPanel(){
    contents += comboBox
    contents += playButton
  }) = Center
}
