package client.window

import java.awt.event.{KeyEvent, KeyListener}
import javax.swing.{JFrame, JOptionPane}
import client.ClientSession
import client.network.ServerHandler
import scala.swing._
import scala.swing.event.ButtonClicked

class LoginWindow(private val previouslyLoggedUser: String) {
  val userField = new TextField(15)
  userField.peer.addKeyListener(new KeyListener {
    override def keyTyped(e: KeyEvent){}

    override def keyPressed(e: KeyEvent){
      if(e.getKeyCode == KeyEvent.VK_ENTER)
        login()
    }
    override def keyReleased(e: KeyEvent){}
  })
  val loginButton = new Button("Login"){
    reactions += {
      case ButtonClicked(b) => login()
    }
  }
  private val window = new Frame(){
    title = ClientSession.Constants.GameName
    peer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    contents = new GridPanel(2,1){
      if(previouslyLoggedUser!=null)
        userField.text = previouslyLoggedUser
      contents += new FlowPanel{
        contents += new Label("Username: ")
        contents += userField
      }
      contents += loginButton
    }
  }
  window.pack()

  private def login(){
    if(userField.text.trim.length > 0 && userField.text.length < 32){
      loginButton.enabled = false
      userField.enabled = false
      val handler = new ServerHandler(userField.text)
      val connectionResult = handler.connect
      if(connectionResult._1){
        val mainWindow = new MainWindow
        ClientSession.SessionVars.mainWindow = mainWindow
        ClientSession.SessionVars.serverHandler = handler
        mainWindow.show()
        dispose()
      }
      else{
        JOptionPane.showMessageDialog(null, connectionResult._2)
        userField.enabled = true
        loginButton.enabled = true
      }
    }
  }

  def dispose() =
    window.dispose()

  def show() {
    window.visible = true
  }
}