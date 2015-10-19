package server.services

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.ArrayBuffer
import common.network.messages.serverToClient.{ResponseToClient, ChatToClient, InfoToClient}
import common.network.messages.clientToServer.{RequestToServer, ChatToServer}


object ChatRooms {
  private val rooms = new ConcurrentHashMap[String, ChatRoom]()
  rooms.put("main", new ChatRoom("main"))

  private class ChatRoom(chatName: String){
    private val users = new ArrayBuffer[String]

    def addUser(user: String){
      this.synchronized{
        users.foreach((u: String)=>{
          val handler = Users.loggedClients.get(u)
          if(handler != null)
            handler.sendMessageToClient(new InfoToClient(InfoToClient.TYPE.CHAT_USER_JOINED, chatName, user))
        })
        users += user
      }
    }

    def removeUser(user: String){
      this.synchronized{
        users -= user
        users.foreach((u: String)=>{
          val handler = Users.loggedClients.get(u)
          if(handler != null)
            handler.sendMessageToClient(new InfoToClient(InfoToClient.TYPE.CHAT_USER_LEFT, chatName, user))
        })
      }
    }

    def getUsers = users

    def sendMessage(msg: ChatToServer, sender: String){
      this.synchronized{
        if(!users.contains(sender))
          return
        val msgToSend = new ChatToClient(sender, chatName, msg.getMessage)
        users.foreach((user: String)=>{
          val handler = Users.loggedClients.get(user)
          if(handler != null)
            handler.sendMessageToClient(msgToSend)
        })
      }
    }

  }
  def removeFromAllChats(username: String){
    val chatKeys = rooms.keys()
    var key: String = ""
    while (chatKeys.hasMoreElements){
      key = chatKeys.nextElement()
      rooms.get(key).removeUser(username)
    }
  }

  def chat(msg: ChatToServer, sender: String){
    msg.getTargetType match{
      case ChatToServer.TARGET.ROOM => {
        val room = rooms.get(msg.getTarget)
        if(room!=null)
          room.sendMessage(msg, sender)
      }
      case ChatToServer.TARGET.PRIVATE =>{
        val msgTarget = Users.loggedClients.get(msg.getTarget)
        if(msgTarget!=null)
          msgTarget.sendMessageToClient(new ChatToClient(sender, msg.getMessage))
      }
    }
  }

  def joinChat(msg: RequestToServer, sender: String) {
    val chat = ChatRooms.rooms.get(msg.getRequestTarget)
    if(chat != null){
      Users.loggedClients.get(sender).sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.OK, msg.getRequestID, chat.getUsers.toArray))
      chat.addUser(sender)
    }
    else{
      Users.loggedClients.get(sender).sendMessageToClient(new ResponseToClient(ResponseToClient.TYPE.DENIED, msg.getRequestID, null))
    }
  }

}
