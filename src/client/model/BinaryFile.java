/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 *
 * @author bram
 */
public class BinaryFile extends LogicalFile {
    
    private String extension;

    public BinaryFile(String name, String extension) {
        super(name + (extension.startsWith(".") ? "" : ".") + extension);
        this.extension = extension;
    }

    @Override
    public String getType() {
        return "Application";
    }

    @Override
    public String toString() {
        return getName().replace(extension, "");
    }
}
