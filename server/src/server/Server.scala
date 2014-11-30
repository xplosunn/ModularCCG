package server

import java.net.ServerSocket

import common.network.NetworkValues
import common.network.messages.clientToServer.RequestToServer
import server.services.{ChatRooms, Games, Users}


object Server extends Thread{
  private val socket: ServerSocket = new ServerSocket(NetworkValues.SERVER_PORT)

  override def run() {
    while (true) {
      val clientSocket = socket.accept()
      new ClientHandler(clientSocket).start()
    }
  }
}