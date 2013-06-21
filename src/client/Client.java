/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import client.control.Controller;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 *
 * @author bram
 */
public class Client extends JFrame {

    private static final String TITLE = "Grid Application Client";
    private static final Dimension SIZE = new Dimension(640, 480);

    public Client(final Controller controller) {
        setTitle(TITLE);
        pack();             //workaround for the tooltip dual-monitor display bug
        setSize(SIZE);
        setResizable(false);

        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDefaultConfiguration().getBounds();
        Point center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        Point topleft = new Point(center.x - 640 / 2, center.y - 480 / 2);
        setLocation(topleft);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(!controller.finishedAllTasks()) {
                    int value = JOptionPane.showConfirmDialog(Client.this, "Some background tasks have not yet finished. Do you want to force these tasks to shutdown?", "Shutdown Tasks", JOptionPane.YES_NO_OPTION);
                    if(value != JOptionPane.OK_OPTION) {
                        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                        return;
                    }
                }
                setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                controller.logout();
            }
        });
    }
}
