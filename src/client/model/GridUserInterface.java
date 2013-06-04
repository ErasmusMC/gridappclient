/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 *
 * @author bram
 */
public enum GridUserInterface {
    
    SARA("ui.grid.sara.nl", "a4:ca:1a:a5:54:30:de:eb:fa:98:c2:e4:ba:0b:9d:a2"),
    EMC("gb-ui-emc.erasmusmc.nl", "0e:b9:b8:37:34:23:d8:aa:7d:9f:02:2b:0b:15:6e:d0");
    
    private String address;
    private String fingerprint;
    
    private GridUserInterface(String address, String fingerprint){
        this.address = address;
        this.fingerprint = fingerprint;
    }
    
    public String getAddress(){
        return address;
    }
    
    public String getFingerprint(){
        return fingerprint;
    }

    @Override
    public String toString() {
        return name() + " (" + getAddress() + ")";
    }
    
}
