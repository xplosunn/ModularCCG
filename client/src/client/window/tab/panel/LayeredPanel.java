package client.window.tab.panel;

import scala.swing.Component;

import javax.swing.*;

/**
 * Created by xs on 15-09-2014.
 */
public class LayeredPanel extends Component{

    private final JLayeredPane peer = new JLayeredPane();

    public LayeredPanel(){
        peer.setLayout(new OverlayLayout(peer));
    }

    @Override
    public JLayeredPane peer() {
        return peer;
    }

    public void add(Component component, int layer){
        peer.add(component.peer(), new Integer(layer));
        repaint();
    }

    public void setLayer(Component component, int layer){
        peer.setLayer(component.peer(), layer);
        repaint();
    }
}
