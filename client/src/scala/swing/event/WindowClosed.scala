package scala.swing.event

import scala.swing.Window

case class WindowClosed(override val source: Window) extends WindowEvent(source)
