package launch

import javax.swing.JLayeredPane

import client.window.tab.panel.LayeredPanel

import scala.swing.{Dimension, Frame, Label}

/**
 * Created by HugoSousa on 09-08-2014.
 */
object TestArray {
  def funcArray{
    val funcArrayBuffer = new Array[(Int)=>Unit](1)
    funcArrayBuffer(0) = (i: Int) => println("yay")
    funcArrayBuffer(0).apply(0)
  }

  def t2 {
    val layeredPanel: LayeredPanel = new LayeredPanel
    layeredPanel.add(new Label("Cenas"), JLayeredPane.DEFAULT_LAYER)
    val frame: Frame = new Frame{
      contents = layeredPanel
      minimumSize = new Dimension(200,300)
    }
    frame.visible = true
  }

}
