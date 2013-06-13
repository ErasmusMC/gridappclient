/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 * This class represents a Logical File Catalog (LFC) reference to a single
 * file stored on a storage element (SE).
 *
 * @author bram
 */
public class LogicalFile extends PersistentObject {

    private static final long serialVersionUID = 3;
    private long diskspace;
    private int progress;

    /**
     * Constructs a FileCatalogRecord to actual grid data resources.
     *
     * @param name filename
     * @param path path to store this {@code TableRecord}
     * @param catalogPath Logical File Catalog path
     */
    public LogicalFile(String name) {
        super(name);
    }
    
    public String getName() {
        return getID();
    }
    
    @Override
    public String getType() {
        return FileType.parse(getID()).toString();
    }

    /**
     * Returns the {@code diskspace} of the actual archive registered under the
     * {@code catalogPath}.
     *
     * @return {@code diskspace} in bytes
     */
    public long getDiskspace() {
        return diskspace;
    }

    /**
     * Returns the {@code progress} of the actual archive registered under the
     * {@code catalogPath} which is being uploaded to the BiG Grid.
     *
     * @return {@code progress} from 0-100 or -1 on error
     */
    public int getStatus() {
        return progress;
    }

    public void setDiskspace(long diskspace) {
        this.diskspace = diskspace;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
    
}
