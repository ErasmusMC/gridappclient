/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.apps.tophat;

import client.model.Application;
import client.model.Batch;
import client.model.Genome;
import client.model.GridStorageElement;
import client.model.Job;
import client.model.JobDescription;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author bram
 */
public class TophatBatch extends Batch {

    protected Genome genome;
    protected Map<Job, Collection<File>> jobs;
    protected String resultSE = GridStorageElement.EMC.getAddress();
    
    protected TophatBatch(String id) {
        super(id, Application.Tophat);

        prerequisites = new ArrayList<>();
        jobs = new TreeMap<>();
    }
    
    @Override
    public Collection<Job> getJobs() {
        return jobs.keySet();
    }

    @Override
    public Collection<File> getInputOf(Job job) {
        return jobs.get(job);
    }

    public void addInputFile(File file) {
        String filename = file.getName();
        String sampleid = filename.substring(0, filename.lastIndexOf('.'));
        if (sampleid.matches(".*_[12]$")) {
            sampleid = sampleid.substring(0, sampleid.lastIndexOf('_'));
        }

        Job job = new Job(this, sampleid, options);

        //pair files on sample ID
        if (!jobs.containsKey(job)) {
            jobs.put(job, new HashSet<File>());
        }
        jobs.get(job).add(file);
    }
    
    public void setGenome(Genome genome) {
        this.genome = genome;
    }

    @Override
    public JobDescription createJobDescription(Job job, String vo) {
        
        if (genome == null || host == null || host.isEmpty() || home == null || home.isEmpty()) {
            return null;
        }
        
        JobDescription description = JobDescription.createNormal(application, vo);
        description.set(JobDescription.Attribute.Arguments, createArguments(job));
        return description;
    }

    /**
     * Creates the command to feed to the wrapper executable.
     *
     * tophatwrapper [tophatoptions] prefix lfc_host lfc_home output_se
     * output_file software genome reads [prerequisites]
     *
     * @param job
     * @return
     */
    private List<String> createArguments(Job job) {
        
        List<String> command = new ArrayList<>();
        //wrapper script
        command.add(application.getWrapper().getName());
        //optional arguments
        for(String option : options.keySet()) {
            command.add(option);
            String value = options.get(option);
            if(!value.isEmpty()) {
                command.add(value);
            }
        }
        
        //mandatory arguments
        command.add(genome.getBowtiePrefix());
        command.add(host);
        command.add(home);
        command.add(resultSE);
        command.add(job.getOutput().getName());
        command.add(binaries.getName());
        command.add(genome.getName());
        command.add(job.getInput().getName());
        
        if(hasPrerequisites()) {
            command.add(getPrequisiteFileName());
        }
        
        return command;
    }

}
