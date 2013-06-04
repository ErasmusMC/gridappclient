/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

/**
 *
 * @author bram
 */
public class ArchiveBuilder {

    public static final String PROGRESS_PROPERTY_NAME = "progress";
    private int progress;
    private Collection<File> files;
    private PropertyChangeListener listener;

    public ArchiveBuilder() {
        files = new ArrayList<>();
        progress = 0;
    }

    public ArchiveBuilder(File... files) {
        this(Arrays.asList(files));
    }

    public ArchiveBuilder(Collection<File> files) {
        this.files = files;
    }

    public long getUncompressedSize() {
        long totalFileSize = 0;
        for (File file : files) {
            totalFileSize += file.length();
        }
        return totalFileSize;
    }

    public void putFile(File file) {
        files.add(file);
    }

    public void putFiles(Collection<File> files) {
        this.files.addAll(files);
    }

    private void setProgress(int progress) {
        if (listener != null && progress > this.progress) {
            int oldValue = this.progress;
            this.progress = progress;
            listener.propertyChange(new PropertyChangeEvent(this, PROGRESS_PROPERTY_NAME, oldValue, progress));
        }
    }

    public void setPropertyChangeListener(PropertyChangeListener listener) {
        this.listener = listener;
    }

    public File createArchive(File destination) throws IOException {

        if (destination.isDirectory()) {
            throw new IllegalArgumentException("File " + destination.getAbsolutePath() + " is a directory!");
        }

        FileOutputStream dest = new FileOutputStream(destination);
        TarOutputStream out = new TarOutputStream(new GZIPOutputStream(new BufferedOutputStream(dest)));

        int bytesTransferred = 0;
        long bytesToTransfer = getUncompressedSize();

        for (File file : files) {
            BufferedInputStream bi = new BufferedInputStream(new FileInputStream(file));
            out.putNextEntry(new TarEntry(file, file.getName()));

            int count;
            byte data[] = new byte[4096];
            while ((count = bi.read(data)) != -1) {
                out.write(data, 0, count);
                if (count != -1) {
                    bytesTransferred += count;
                    setProgress((int) Math.min(100 * (float)bytesTransferred / bytesToTransfer, 100));
                }
            }
            out.flush();
            bi.close();
        }
        out.close();
        return destination;
    }
}
