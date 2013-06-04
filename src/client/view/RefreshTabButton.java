/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.control.Controller;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

/**
 *
 * @author bram
 */
public class RefreshTabButton extends JPanel {

    private static final int REQUIRED_PROXY_TIME = 6;
    private static final int DEFAULT_PROXY_TIME = 12;
    private Timer timer;

    public RefreshTabButton(final JTabbedPane parent, final Controller controller, String title, final int index) {
        super(new FlowLayout(FlowLayout.CENTER, 5, 0));

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        add(new JLabel(title, JLabel.LEFT));

        Icon icon = new ImageIcon(getClass().getResource("/images/refresh-icon.png"));
        final JButton button = new JButton(icon);
        button.setBorder(null);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(15, 15));
        button.setToolTipText("Refresh job statuses");
        add(button);

        timer = new Timer(60000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button.setEnabled(true);
                button.setToolTipText("Refresh job statuses");
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                button.setEnabled(false);
                button.setToolTipText("This action will be available again within 1 minute.");
                timer.restart();

                parent.setSelectedIndex(index);
                if (!controller.getJobs().isEmpty()) {

                    if (controller.getLocalProxyLifetime() < REQUIRED_PROXY_TIME) {
                        PasswordDialog dialog = new PasswordDialog(RefreshTabButton.this, "Local Proxy Required!");
                        dialog.setVisible(true);

                        if (dialog.getValue() != JOptionPane.OK_OPTION) {
                            dialog.dispose();
                            return;
                        }

                        boolean succes;
                        try {
                            succes = controller.createLocalProxy(dialog.getPassword(), DEFAULT_PROXY_TIME);
                        } catch (IOException | InterruptedException ex) {
                            JOptionPane.showMessageDialog(RefreshTabButton.this, ex.getMessage(), "I/O Error occured", JOptionPane.WARNING_MESSAGE);
                            Logger.getLogger(NewFilePanel.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                            return;
                        } finally {
                            dialog.dispose();
                        }

                        if (!succes) {
                            JOptionPane.showMessageDialog(RefreshTabButton.this, "Invalid passphrase!", "Authentication Error", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }

                    controller.updateJobStats();
                }
            }
        });
    }
}
