/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 *
 * @author bram
 */
public class Genome extends LogicalFile {
    
    private final Assembly assembly;
    private final String prefix;

    public Genome(Assembly assembly, String prefix) {
        super(assembly.toFileName());
        this.assembly = assembly;
        this.prefix = prefix;
    }
    
    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    public Assembly getAssembly() {
        return assembly;
    }
    
    public String getBowtiePrefix() {
        return prefix;
    }
}
