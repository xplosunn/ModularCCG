package client

import javax.swing.JOptionPane

import client.network.ServerHandler
import client.window.{LoginWindow, MainWindow}

/**
 * Contém todas as variáveis relevantes a uma sessão do lado do utilizador,
 * para que no futuro seja possível reconectar no caso de a ligação cair.
 */
object ClientSession {
  object Constants{
    val GameName = "Yet Nameless CCG v0.1.0 Pre-Alfa"
  }
  object SessionVars{
    var mainWindow: MainWindow = null
    var serverHandler: ServerHandler = null
  }

  def mainWindow = SessionVars.mainWindow

  def disconnect(){
    val window = SessionVars.mainWindow
    SessionVars.mainWindow = null
    window.dispose()
    val userName = SessionVars.serverHandler.username
    SessionVars.serverHandler = null
    new LoginWindow(userName).show()
    JOptionPane.showMessageDialog(null,"Connection to server lost.")
  }
}
