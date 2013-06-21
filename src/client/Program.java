/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import client.control.Controller;
import client.control.NavigationController;
import client.control.NavigationController.View;
import client.model.Application;
import client.model.BinaryFile;
import client.view.FileTab;
import client.view.JobTab;
import client.view.LoginPanel;
import client.view.RefreshTabButton;
import java.io.File;
import javax.swing.JTabbedPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 *
 * @author bram
 */
public class Program {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        //<editor-fold defaultstate="collapsed" desc=" Set customized Nimbus look and feel ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            System.out.println(ex.getMessage());
        }

        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        defaults.put("nimbusOrange", defaults.get("nimbusGreen"));
        //</editor-fold>

        // applications
        // the file is not really added? Load from jar
        Application.Tophat.addBinaryFile(new BinaryFile("tophat-2.0.8b", ".tar.gz"), new File(Program.class.getResource("apps/tophat/tophat-2.0.8b.tar.gz").getPath()));

        // controllers
        final Controller controller = new Controller();
        final Client client = new Client(controller);
        NavigationController navigator = new NavigationController(client);

        // views
        LoginPanel loginPanel = new LoginPanel(controller, navigator);
        final JTabbedPane tableViewer = new JTabbedPane();
        tableViewer.addTab("Jobs", new JobTab(navigator, controller));
        tableViewer.addTab("Files", new FileTab(controller));

        final int index = tableViewer.indexOfTab("Jobs");
        RefreshTabButton tabButton = new RefreshTabButton(tableViewer, controller, "Jobs", index);
        tableViewer.setTabComponentAt(index, tabButton);

        // define panels to navigate to
        navigator.put(View.LOGIN, loginPanel);
        navigator.put(View.TABLE, tableViewer);
        navigator.navigate(null, View.LOGIN);

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                client.setVisible(true);
            }
        });
    }
}
