/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 *
 * @author bram
 */
public enum WallClock {

    Express(30),
    Short(240),
    Medium(2160),
    Long(4320);
    private int minutes;

    private WallClock(int minutes) {
        this.minutes = minutes;
    }

    public int getTime() {
        return minutes;
    }
}
