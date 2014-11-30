package client.window.tab.panel;

import client.ClientSession;
import client.window.MainWindow;
import scala.Tuple2;
import scala.swing.Component;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class DefenseLines extends Component {
    ArrayList<Tuple2<GraphicalRemoteCard,GraphicalRemoteCard>> defenses = new ArrayList<Tuple2<GraphicalRemoteCard, GraphicalRemoteCard>>();
    private final GraphicalLines peer = new GraphicalLines();

    public void add(GraphicalRemoteCard attacker, GraphicalRemoteCard defender){
        defenses.add(new Tuple2<GraphicalRemoteCard, GraphicalRemoteCard>(attacker, defender));
        repaint();
    }

    public void removeDefender(GraphicalRemoteCard blocker){
        for (Tuple2<GraphicalRemoteCard, GraphicalRemoteCard> defense : defenses)
            if(defense._2().equals(blocker)){
                defenses.remove(defense);
                repaint();
                return;
            }
    }

    public void clear(){
        defenses = new ArrayList<Tuple2<GraphicalRemoteCard, GraphicalRemoteCard>>();
        repaint();
    }

    public boolean isDefender(GraphicalRemoteCard defender){
        for (Tuple2<GraphicalRemoteCard, GraphicalRemoteCard> defense : defenses)
            if(defense._2().equals(defender))
                return true;
        return false;
    }

    @Override
    public JComponent peer() {
        return peer;
    }

    class GraphicalLines extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            MainWindow mainWindow = ClientSession.mainWindow();
            if (mainWindow != null) {
                JFrame frame = mainWindow.frame().peer();
                for (Tuple2<GraphicalRemoteCard, GraphicalRemoteCard> defense : defenses) {
                    Rectangle r1 = SwingUtilities.convertRectangle(defense._1().peer().getParent(), defense._1().bounds(), frame);
                    Rectangle r2 = SwingUtilities.convertRectangle(defense._2().peer().getParent(), defense._2().bounds(), frame);

                    Point p1 = new Point(r1.x + r1.width / 2, r1.y);
                    Point p2 = new Point(r2.x + r2.width / 2, r2.y);
                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }
}