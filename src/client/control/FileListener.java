/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.control;

/**
 *
 * @author bram
 */
public interface FileListener<T> {

    void fileDeleted(T row);
    void fileAdded(T row);
}
