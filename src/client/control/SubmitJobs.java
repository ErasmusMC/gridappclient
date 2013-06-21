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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
                        LOGGER.log(Level.INFO, "Start upload {0}", job.getID());
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

                        File input = new File(tempdir, job.getInput().getID());
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
                        LOGGER.log(Level.INFO, "Finished upload {0}", job.getID());

                    } catch (IOException | InterruptedException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        job.setProgress(-1);
                    }
                }
            };
            childs.add(task);
            controller.addJob(task, job);
        }

        //upload binaries
        controller.executeInBackground(new Thread() {
            @Override
            public void run() {
                try {
                    BinaryFile binaries = batch.getBinaries();
                    if (!controller.exists(binaries) && batch.getApplication().getVersions().contains(binaries)) {
                        LOGGER.log(Level.INFO, "Start upload {0}", batch.getApplication().getBinaryFile(binaries).getName());
                        //copy binaries from within jarfile (stream) to a temp file
                        File binaryFile = new File(controller.getTemporaryDirectory(), batch.getApplication().getBinaryFile(binaries).getName());
                        if (!binaryFile.exists()) {
                            int bytesRead;
                            byte[] buffer = new byte[4096];
                            FileOutputStream out = new FileOutputStream(binaryFile);
                            InputStream in = batch.getApplication().getWrapperAsStream();
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            in.close();
                            out.close();
                        }
                        controller.addFile(this, binaries, binaryFile);
                        LOGGER.log(Level.INFO, "Finished upload {0}", batch.getApplication().getBinaryFile(binaries).getName());
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    SubmitJobs.this.interruptChilds();
                } finally {
                    latch.countDown();
                }
            }
        });

        //upload prerequisites
        controller.executeInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    if (batch.hasPrerequisites()) {
                        LOGGER.log(Level.INFO, "Start upload {0}", batch.getPrequisiteFileName());
                        ArchiveBuilder archiver = new ArchiveBuilder();
                        archiver.putFiles(batch.getPrerequisites());

                        File filename = new File(tempdir, batch.getPrequisiteFileName());
                        File prerequisitesArchive = archiver.createArchive(filename);
                        controller.uploadToSE(prerequisitesArchive, true);
                        LOGGER.log(Level.INFO, "Finished upload {0}", batch.getPrequisiteFileName());
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
        controller.executeInBackground(new Thread() {
            @Override
            public void run() {
                try {
                    LOGGER.log(Level.INFO, "Start upload {0}", batch.getApplication().getWrapperName());
                    //copy the wrapper from within jarfile (stream) to a temp file
                    File wrapper = new File(controller.getTemporaryDirectory(), batch.getApplication().getWrapperName());
                    if (!wrapper.exists()) {
                        int bytesRead;
                        byte[] buffer = new byte[4096];
                        FileOutputStream out = new FileOutputStream(wrapper);
                        InputStream in = batch.getApplication().getWrapperAsStream();
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        in.close();
                        out.close();
                    }
                    controller.uploadToUI(wrapper);
                    LOGGER.log(Level.INFO, "Finished upload {0}", batch.getApplication().getWrapperName());
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

        if (interrupted) {
            return;
        }

        interrupted = true;

        for (Job job : batch.getJobs()) {
            controller.removeJob(job);
        }
    }
}
