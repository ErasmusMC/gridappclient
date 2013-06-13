/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import client.apps.tophat.TophatOptionForm;
import client.control.Controller;
import client.control.FileListener;
import client.control.NavigationController;
import java.awt.Container;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author bram
 */
public enum Application implements FileListener<BinaryFile> {

    Tophat("/client/apps/tophat/tophatwrapper.sh", 168, WallClock.LONG.getTime(), 8) {
        @Override
        public Container createForm(BinaryFile version, NavigationController navigator, Controller controller) {
            TophatOptionForm form = new TophatOptionForm(this, version, navigator, controller);
            controller.addFileListener(LogicalFile.class, form);
            return form;
        }
    };
    private String wrapperName, wrapperPath;
    private int lifetime, runtime, cores;
    private Map<BinaryFile, File> binaries = new TreeMap<>();

    private Application(String wrapper, int lifetime, int runtime, int threads) {

        File file = new File(wrapper);

        this.wrapperName = file.getName();
        this.wrapperPath = file.getPath();
        this.lifetime = lifetime;
        this.runtime = runtime;
        this.cores = threads;
    }

    public abstract Container createForm(BinaryFile version, NavigationController navigator, Controller controller);

    public InputStream getWrapperAsStream() {
        return Application.class.getResourceAsStream(wrapperPath);
    }

    public String getWrapperName() {
        return wrapperName;
    }

    /**
     * Returns the maximum proxy lifetime necessary to complete this application
     * it's jobs. Beware: This value should be larger than the maximum runtime
     * of a job, because proxy lifetime does not pause whilst in a waiting
     * queue!
     *
     * @return lifetime in hours
     */
    public int getMaximumProxyLifetime() {
        return lifetime;
    }

    /**
     * Returns the maximum runtime of this application on the Grid.
     *
     * @return time in hours
     */
    public int getMaximumRuntime() {
        return runtime;
    }

    /**
     * Returns the minimal number of threads this application requires.
     *
     * @return number of threads
     */
    public int getNumberOfThreads() {
        return cores;
    }

    /**
     * Returns local binaries.
     *
     * @param file archive with binaries
     * @return
     */
    public File getBinaryFile(BinaryFile file) {
        return binaries.get(file);
    }

    /**
     * Add a version of this application.
     *
     * @param version
     */
    public void addVersion(BinaryFile version) {
        addBinaryFile(version, null);
    }

    /**
     * Add a new version of this application and provide local binaries.
     *
     * @param binaries link to the LFC location
     * @param file local archive with binaries
     */
    public void addBinaryFile(BinaryFile binaries, File file) {
        this.binaries.put(binaries, file);
    }

    /**
     * Returns this versions of application.
     *
     * @return application versions
     */
    public Collection<BinaryFile> getVersions() {
        return binaries.keySet();
    }

    @Override
    public void fileDeleted(BinaryFile row) {
        if (row.getID().startsWith(toString().toLowerCase())) {
            binaries.remove(row);
        }
    }

    @Override
    public void fileAdded(BinaryFile row) {
        if (row.getID().startsWith(toString().toLowerCase())) {
            binaries.put(row, null);
        }
    }
}
