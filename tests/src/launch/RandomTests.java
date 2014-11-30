package launch;
import client.window.tab.panel.LayeredPanel;
import scala.swing.Frame;
import scala.swing.Label;

import javax.swing.*;
import java.awt.*;

public class RandomTests {
    private static void t1(){
        JFrame frame = new JFrame("");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));
        frame.getContentPane().add(panel);
        panel.add(new JButton("1"));
        panel.add(new JButton("2"));
        frame.setVisible(true);
    }

    private static void t2(){
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(300,200));
        //layeredPane.setLayout(null);
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        layeredPane.setBorder(BorderFactory.createTitledBorder(
                "Layers inside"));
        JLabel l2 = new JLabel("<-- Yay working -->");
        l2.setOpaque(false);
        l2.setVerticalAlignment(JLabel.TOP);
        l2.setHorizontalAlignment(JLabel.CENTER);
        layeredPane.add(l2, new Integer(1));

        JLabel l1 = new JLabel("<--                                        -->");
        l1.setOpaque(true);
        l1.setBackground(new Color(0,230,230));
        l1.setVerticalAlignment(JLabel.TOP);
        l1.setHorizontalAlignment(JLabel.CENTER);
        layeredPane.add(l1, new Integer(0));



        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setOpaque(true);
        panel.add(layeredPane);
        JFrame frame = new JFrame("wow");
        frame.getContentPane().add(panel);
        frame.setSize(new Dimension(300,200));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }


    public static void main(String[] args){
        //t1();
        //TestArray.funcArray();
        t2();
        //TestArray.t2();
        //LayeredPaneDemo.main(null);
    }
}
