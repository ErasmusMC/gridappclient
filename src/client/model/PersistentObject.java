/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extend this class to store objects on the Grid UI host.
 *
 * @author bram
 */
public abstract class PersistentObject implements Comparable<PersistentObject>, Serializable {

    public transient static final String SERIALIZED_SUFFIX = ".ser";
    protected transient static final Logger LOGGER = Logger.getLogger(PersistentObject.class.getSimpleName());
    private String id;

    /**
     * Constructs a PersistentObject which can be serialized and stored to a
     * file on a Grid UI host, also see {@link client.util.SSH}.upload(...).
     *
     * @param name the filename to store this object to
     * @param path the path to store this object to
     */
    public PersistentObject(String name) {

        if (name == null) {
            throw new NullPointerException("Name can't be null!");
        }
        
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Name can't contain spaces!");
        }

        this.id = name;
    }

    public String getID() {
        return id;
    }
    
    public String getSerializedFileName() {
        return id + SERIALIZED_SUFFIX;
    }

    public String getType() {
        return getClass().getSimpleName();
    }
    
    /**
     * Serialize this object to file.
     *
     */
    public void saveToFile(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(this);
            out.flush();
            out.close();
            fos.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public int compareTo(PersistentObject o) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PersistentObject)) {
            return false;
        }

        PersistentObject o = (PersistentObject) obj;
        return obj == this || id.equals(o.id);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
    @Override
    public String toString() {
        return getID();
    }
}
