package server.services

import java.util.concurrent.ConcurrentHashMap

import server.ClientHandler

object Users {
  val loggedClients = new ConcurrentHashMap[String, ClientHandler]()

  def login(username: String, handler: ClientHandler): Boolean = {
    val loggedClientUsernames = loggedClients.keys()
    var key: String = ""
    while (loggedClientUsernames.hasMoreElements){
      key = loggedClientUsernames.nextElement()
      if(key.equals(username))
        return false
    }
    loggedClients.put(username, handler)
    true
  }

  def removeLoggedClient(handler: ClientHandler){
    ChatRooms.removeFromAllChats(handler.userName)
    Games.disconnect(handler)
    loggedClients.remove(handler.userName)
  }

  def onlineCount: Int = loggedClients.size()
}
