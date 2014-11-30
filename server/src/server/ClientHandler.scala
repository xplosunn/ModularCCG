package server

import java.io.{IOException, ObjectInputStream, ObjectOutput, ObjectOutputStream}
import java.net.Socket

import common.network.messages.clientToServer._
import common.network.messages.serverToClient.{LoginResponse, MessageToClient}
import server.services.{ErrorLog, Users, ChatRooms, Games}

class ClientHandler(private val socket: Socket) extends Thread{
  protected var userName: String = null
  
  def getUserName = userName
  
  private var objOut: ObjectOutput = null

  override def run() {
    try {
      //Setup streams
      val objIn = new ObjectInputStream(socket.getInputStream)
      objOut = new ObjectOutputStream(socket.getOutputStream)
      //Login
      val loginMsg: LoginRequest = objIn.readObject() match{case msg: LoginRequest => msg ; case _ => null}
      if(loginMsg == null)
        return
      //Validate login
      if(Users.login(loginMsg.getUser, this)){
        userName = loginMsg.getUser
        objOut.writeObject(new LoginResponse(LoginResponse.TYPE.SUCCESS ))
        while (true) {
          try {
            val in: MessageToServer = objIn.readObject match {case m: MessageToServer => m ; case _ => null}
            handleMessage(in)
          }
          catch {
            case e: ClassNotFoundException => ErrorLog.unknownNetworkMessage(userName,e)
          }
        }
      }
      else
        objOut.writeObject(new LoginResponse(LoginResponse.TYPE.ALREADY_LOGGED))
    } catch {
      case e: IOException => if(userName != null) Users.removeLoggedClient(this)
    }
  }

  protected def handleMessage(message: MessageToServer) {
    message match{
      case message: ChatToServer => ChatRooms.chat(message, userName)
      case message: RequestToServer => clientRequest(message)
      case message: GameAction => Games.gameAction(message, userName)
    }
  }

  private def clientRequest(msg: RequestToServer){
    msg.getRequest match{
      case RequestToServer.REQUEST.JOIN_CHAT =>
        ChatRooms.joinChat(msg, userName)
      case RequestToServer.REQUEST.JOIN_GAME_QUEUE =>
        Games.queueForDuel(this, msg)
    }
  }

  def sendMessageToClient(message: MessageToClient): Boolean = {
    try {
      objOut.writeObject(message)
      true
    }
    catch {
      case e: IOException => {
        false
      }
    }
  }
}