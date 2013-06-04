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

    EXPRESS(30),
    SHORT(240),
    MEDIUM(2160),
    LONG(4320);
    private int minutes;

    private WallClock(int minutes) {
        this.minutes = minutes;
    }

    public int getTime() {
        return minutes;
    }
}
