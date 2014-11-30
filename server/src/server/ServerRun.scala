package server

import java.text.NumberFormat
import java.util.Scanner

import server.services.Users

/**
 * Created by HugoSousa on 31-08-2014.
 */
object ServerRun {
  val nl = System.lineSeparator

  def main(args: Array[String]){
    val s: Scanner = new Scanner(System.in)
    Server.start()
    System.out.println("Server started.")
    while(true){
      print(">")
      command(s.nextLine().split(' '))
    }

  }

  def command(cmd: Array[String]){
    if(cmd.size > 0)
      cmd(0) match {
        case "?" => help
        case "help" => help
        case "resources" => getResources
        case "users" => users(cmd)
        case "exit" => System.exit(0)
        case "quit" => System.exit(0)
        case _ => println("unknown command")
      }
  }

  def help {
    println("Commands: " + nl
      + ">users" + nl
      + ">resources" + nl
      + ">exit")
  }

  def users(cmd: Array[String]) {
    def userHelp {
      println("users options: " + nl
        + ">count" + nl
        + ">print")
    }
    if (cmd.size > 1)
      cmd(1) match {
        case "help" => userHelp
        case "?" => userHelp
        case "count" => println("" + Users.onlineCount)
        case "print" =>
          val keys = Users.loggedClients.keys()
          while (keys.hasMoreElements) {
            println(keys.nextElement())
          }
        case _ => println("unknown users option")
      }

  }

  def getResources {
    val runtime: Runtime = Runtime.getRuntime
    val format: NumberFormat = NumberFormat.getInstance
    val maxMemory: Long = runtime.maxMemory
    val allocatedMemory: Long = runtime.totalMemory
    val freeMemory: Long = runtime.freeMemory
    var sb = "free memory: " + format.format(freeMemory / 1024) + nl
    sb += "allocated memory: " + format.format(allocatedMemory / 1024) + nl
    sb += "max memory: " + format.format(maxMemory / 1024) + nl
    sb += "total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024)
    println(sb)
  }
}
