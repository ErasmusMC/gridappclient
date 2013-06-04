/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.control;

import client.Client;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author bram
 */
public class NavigationController {

    public enum View {

        LOGIN,
        TABLE;
    }
    private Client client;
    private Map<View, Container> navigationMap = new EnumMap<>(View.class);

    public NavigationController(Client client) {
        this.client = client;
    }

    public void put(View key, Container view) {
        navigationMap.put(key, view);
    }

    public void remove(View key) {
        navigationMap.remove(key);
    }

    public void navigate(Container from, View key) {
        if (from != null) {
            client.remove(from);
        }

        Container to = navigationMap.get(key);
        if (to != null) {
            client.getContentPane().add(to, BorderLayout.CENTER);
            to.setVisible(true);
        }
        client.revalidate();
        client.repaint();
    }

    public void navigate(Container from, Container to) {
        if (from != null) {
            if (navigationMap.values().contains(from)) {
                from.setVisible(false);
            } else {
                client.remove(from);
            }
        }

        client.add(to);
        to.setVisible(true);
    }
}
