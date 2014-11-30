package unit.common.network.messages

import java.util

import client.network.ServerHandler
import common.network.messages.serverToClient.MessageToClient
import java.util.ArrayList

/**
 * Created by HugoSousa on 16-10-2014.
 */
class FakeServerHandler(username: String) extends ServerHandler(username) {
  val recievedMessages = new ArrayList[MessageToClient]

  override def handleMessage(message: MessageToClient) {
    recievedMessages.add(message)
  }
  
}