/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author bram
 */
public abstract class Batch {

    public static final String PREREQUISITE_POSTFIX = "_prerequisites";
    protected Application application;
    protected String id, host, home;
    protected BinaryFile binaries;
    protected Map<String, String> options;
    protected List<File> prerequisites;

    protected Batch(String id, Application applcation) {
        this.id = id;
        this.application = applcation;

        if (id == null || id.isEmpty() || application == null) {
            throw new IllegalArgumentException("Invalid Argument: Cannot create Batch!");
        }

        options = new TreeMap<>();
        prerequisites = new ArrayList<>();
    }

    public abstract Collection<Job> getJobs();

    public abstract Collection<File> getInputOf(Job job);

    public abstract JobDescription createJobDescription(Job job, String vo);

    public Application getApplication() {
        return application;
    }

    public BinaryFile getBinaries() {
        return binaries;
    }

    public String getPrequisiteFileName() {
        return id + PREREQUISITE_POSTFIX;
    }

    public Collection<File> getPrerequisites() {
        return prerequisites;
    }
    
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }
    
    public void addOption(String option) {
        options.put(option, "");
    }

    public void addOption(String option, String value) {
        options.put(option, value);
    }
    
    public void addOption(String option, int value) {
        options.put(option, String.valueOf(value));
    }

    public void addPrerequisiteFiles(File... file) {
        prerequisites.addAll(Arrays.asList(file));
    }
    
    public boolean containsOption(String option) {
        return options.containsKey(option);
    }
    
    public void setHostLFC(String host) {
        this.host = host;
    }

    public void setHomePathLFC(String path) {
        home = path;
    }

    public void setSoftwareBinaries(BinaryFile binaries) {
        this.binaries = binaries;
    }
}
