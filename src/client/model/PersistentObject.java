/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Extend this class to store objects on the Grid UI host.
 *
 * @author bram
 */
public abstract class PersistentObject implements Comparable<PersistentObject>, Serializable {

    private String name;
    protected transient static final Logger LOGGER = Logger.getLogger(PersistentObject.class.getSimpleName());
    protected transient PropertyChangeSupport propertyChangeSupport;

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

        this.name = name;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
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
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PersistentObject)) {
            return false;
        }

        PersistentObject o = (PersistentObject) obj;
        return obj == this || new EqualsBuilder().append(name, o.name).isEquals();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }
}
