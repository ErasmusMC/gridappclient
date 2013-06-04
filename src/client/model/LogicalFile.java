/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import client.view.FileChooser;

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
    
    @Override
    public String getType() {
        return FileChooser.parse(getName()).getDisplayName();
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

        long oldDiskspace = this.diskspace;
        this.diskspace = diskspace;
        firePropertyChange("diskspace", oldDiskspace, diskspace);
    }

    public void setProgress(int progress) {

        int oldProgress = this.progress;
        this.progress = progress;

        if (oldProgress != progress) {
            firePropertyChange("", oldProgress, progress);
        }
    }
    
}
