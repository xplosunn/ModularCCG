package client.window

import scala.swing.{Component, FileChooser}
import scala.swing.FileChooser.Result._
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.{FileInputStream, ObjectInputStream, FileOutputStream, ObjectOutputStream}
import scala.collection.mutable.ArrayBuffer
import common.card.{Deck, Card}

object DeckChooser {
  private val DECK_FILE_EXTENSION = "mccgdeck"
  //private val DECK_FOLDER_PATH = "decks"
  private val fileChooser = new FileChooser()
  fileChooser.fileFilter_=(new FileNameExtensionFilter("Deck files", DECK_FILE_EXTENSION))


  def load(parent: Component): Deck = {
    if(fileChooser.showOpenDialog(parent) == Approve){
      var streamIn: FileInputStream = null
      var objInStream: ObjectInputStream = null
      var deck: Deck = null
      try {
        val file = fileChooser.selectedFile
        streamIn = new FileInputStream(file.getAbsolutePath)
        objInStream = new ObjectInputStream(streamIn)
        deck = objInStream.readObject() match {
          case obj: Deck => obj;
          case _ => null
        }
      } catch {
        case _: Throwable =>
      } finally {
        objInStream.close()
        streamIn.close()
      }
      return deck
    }
    null
  }

  def save(parent: Component, cards: ArrayBuffer[(Card, Int)]): Boolean = {
    val deck = new Deck()
    for(i<-cards.indices)
      deck.add(cards(i)._1, cards(i)._2)

    if(!deck.validate)
      false
    else if (fileChooser.showSaveDialog(parent) == Approve) {
      val file = fileChooser.selectedFile
      var path = file.getAbsolutePath
      if (!path.endsWith("." + DECK_FILE_EXTENSION))
        path += "." + DECK_FILE_EXTENSION
      var fileOut: FileOutputStream = null
      var objOut : ObjectOutputStream = null
      try {
        fileOut = new FileOutputStream(path)
        objOut =new ObjectOutputStream(fileOut)
        objOut.writeObject(deck)
        objOut.close()
        fileOut.close()
        true
      } catch {
        case _: Throwable =>
          objOut.close()
          fileOut.close()
          false
      }
    }
    else
      false
  }
}