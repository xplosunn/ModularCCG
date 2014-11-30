package client.window.tab

import client.ClientSession

import scala.swing._
import scala.collection.mutable.ArrayBuffer
import scala.swing.event.{KeyPressed, Key}
import scala.swing.TabbedPane.Page

object LobbyTab extends BorderPanel{
  import BorderPanel.Position._

  class ChatTab extends BorderPanel{

    private val usersInChat = new ArrayBuffer[String]()
    val chatTextArea = new TextArea{
      editable = false
      background = new Color(240,240,240)
    }
    layout(chatTextArea) = Center

    val usersTextArea = new TextArea{
      text = "Others in chat:   "
      editable = false
      background = new Color(210,210,210)
    }
    layout(usersTextArea) = East

    def addChatUser(user: String){
      usersInChat += user
      updateUsersTextArea()
    }

    def removeChatUser(user: String){
      usersInChat -= user
      updateUsersTextArea()
    }

    def addChatMessage(sender: String, message: String){
      chatTextArea.text_=(chatTextArea.text + System.lineSeparator() + sender + "> " + message)
    }

    def updateUsersTextArea(){
      usersTextArea.text_=("Users in chat:   ")
      for(i<-usersInChat.indices)
        usersTextArea.text_=(usersTextArea.text + System.lineSeparator() + usersInChat(i))
      usersTextArea.repaint()
    }

  }

  layout(new TextField{
    listenTo(keys)
    reactions += {
      case KeyPressed(_, Key.Enter, _,_) =>
        val msgContent = text
        text = ""
        if(msgContent.trim != ""){
          val serverHandler = ClientSession.SessionVars.serverHandler
          if(serverHandler != null)
            serverHandler.Chat.sendMessage(msgContent)
        }

    }
  }) = South

  val chatTabs = new ArrayBuffer[ChatTab]
  val tabbedPane = new TabbedPane{
    tabPlacement = Alignment.Left
  }
  layout(tabbedPane) = Center

  def addChatUser(chat: String, user: String){
    for(i<-chatTabs.indices){
      if(chatTabs(i).name == chat){
        chatTabs(i).addChatUser(user)
        return
      }
    }
  }

  def addChatMessage(chat: String, sender: String, message: String){
    for(i<-chatTabs.indices){
      if(chatTabs(i).name == chat){
        chatTabs(i).addChatMessage(sender, message)
        return
      }
    }
  }

  def addChat(name: String, users: ArrayBuffer[String]){
    val newTab = new ChatTab
    newTab.name = name
    users.foreach((user: String)=>{newTab.addChatUser(user)})
    chatTabs += newTab
    tabbedPane.pages += new Page("#" + name,newTab)
  }
}