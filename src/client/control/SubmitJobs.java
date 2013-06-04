/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.control;

import client.model.ArchiveBuilder;
import client.model.Batch;
import client.model.BinaryFile;
import client.model.Job;
import client.model.JobDescription;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bram
 */
public class SubmitJobs extends Thread {

    private static final Logger LOGGER = Logger.getLogger(SubmitJobs.class.getSimpleName());
    private boolean interrupted = false;
    private Controller controller;
    private List<Thread> childs;
    private final Batch batch;

    public SubmitJobs(Controller controller, Batch batch) {
        this.controller = controller;
        this.batch = batch;
        this.childs = new ArrayList<>();
    }

    @Override
    public void run() {

        final File tempdir = controller.getTemporaryDirectory();
        final CountDownLatch latch = new CountDownLatch(3);

        //create tasks
        for (final Job job : batch.getJobs()) {
            Thread task = new Thread() {
                @Override
                public void run() {
                    try {
                        job.setProgress(1);
                        controller.notifyJobChanged(job);

                        ArchiveBuilder archiver = new ArchiveBuilder(batch.getInputOf(job));
                        archiver.setPropertyChangeListener(new PropertyChangeListener() {
                            @Override
                            public void propertyChange(PropertyChangeEvent evt) {
                                if (evt.getPropertyName().equals(ArchiveBuilder.PROGRESS_PROPERTY_NAME)) {
                                    job.setProgress((int) evt.getNewValue() / 2);
                                    controller.notifyJobChanged(job);
                                }
                            }
                        });

                        File input = new File(tempdir, job.getInput().getName());
                        archiver.createArchive(input);
                        job.setProgress(50);
                        controller.notifyJobChanged(job);

                        controller.uploadToSE(input, false);
                        job.setProgress(95);
                        controller.notifyJobChanged(job);

                        JobDescription description = batch.createJobDescription(job, controller.getVirtualOrganisation());

                        File jobFile = new File(tempdir, job.getJobFileName());
                        description.saveToFile(jobFile);
                        controller.uploadToUI(jobFile);

                        latch.await();

                        String id = controller.submitJob(jobFile.getName());
                        if (id == null) {
                            throw new InterruptedException("Error on job submit!");
                        }

                        job.setId(id);
                        job.setProgress(100);
                        job.setStatus("Submitted");
                        controller.notifyJobChanged(job);
                        controller.uploadToUI(job);

                    } catch (IOException | InterruptedException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        job.setProgress(-1);
                    }
                }
            };
            
            controller.addJob(task, job);
            childs.add(task);
        }

        //upload binaries
        controller.executeInBackground(new Runnable() {
            @Override
            public void run() {
                BinaryFile binaries = batch.getBinaries();
                if (!controller.exists(binaries) && batch.getApplication().getVersions().contains(binaries)) {
                    controller.addFile(binaries, batch.getApplication().getBinaryFile(binaries));
                }
                latch.countDown();
            }
        });

        //upload prerequisites
        controller.executeInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    if (batch.hasPrerequisites()) {
                        ArchiveBuilder archiver = new ArchiveBuilder();
                        archiver.putFiles(batch.getPrerequisites());

                        File filename = new File(tempdir, batch.getPrequisiteFileName());
                        File prerequisitesArchive = archiver.createArchive(filename);
                        controller.uploadToSE(prerequisitesArchive, true);
                    }
                } catch (InterruptedException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    SubmitJobs.this.interruptChilds();
                } finally {
                    latch.countDown();
                }
            }
        });

        //upload wrapper
        controller.executeInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    controller.uploadToUI(batch.getApplication().getWrapper());
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    SubmitJobs.this.interruptChilds();
                } finally {
                    latch.countDown();
                }
            }
        });

        //execute tasks
        for (Thread t : childs) {
            controller.executeInBackground(t);
        }
    }
    
    /**
     * Called when any required file has failed to upload.
     */
    private synchronized void interruptChilds() {
        
        if(interrupted) {
            return;
        }
        
        interrupted = true;
        
        for (Thread t : childs) {
            t.interrupt();
        }

        for (Job job : batch.getJobs()) {
            if (job.isSubmitted()) {
                try {
                    controller.cancelJob(job);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            controller.removeJob(job);
        }
    }
}
