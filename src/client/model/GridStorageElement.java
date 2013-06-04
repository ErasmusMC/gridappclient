/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 *
 * @author bram
 */
public enum GridStorageElement {
    
    EMC("gb-se-emc.erasmusmc.nl");
    private String address;
    
    private GridStorageElement(String address) {
        this.address = address;
    }
    
    public String getAddress() {
        return address;
    }
    
}
