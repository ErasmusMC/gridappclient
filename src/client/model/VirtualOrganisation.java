/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 * Note: lowercase names required as argument to commands.
 * 
 * @author bram
 */
public enum VirtualOrganisation {
    
    LSGRID;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
