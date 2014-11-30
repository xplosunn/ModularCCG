package server.services

/**
 * Created by Hugo on 15-06-2014.
 */
object ErrorLog {
  def unknownNetworkMessage(userName: String, e: ClassNotFoundException){
    e.printStackTrace()
  }

}
