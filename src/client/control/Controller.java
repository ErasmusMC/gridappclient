/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.control;

import client.model.Application;
import client.model.ArchiveBuilder;
import client.model.BinaryFile;
import client.model.Job;
import client.model.LogicalFile;
import client.model.PersistentObject;
import client.model.RowTableModel;
import client.model.VirtualOrganisation;
import client.view.BrowseOptionDialog;
import client.view.FileChooserFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;

/**
 *
 * @author bram
 */
public class Controller {

    private static final String LFC_HOST = "lfc.grid.sara.nl";
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getSimpleName());
    private static final String PWD = ".emcgridclient/";
    private static final String OBJECT_DIR = PWD + "objects/";
    private static final String FILES_DIR = PWD + "files/";
    private static final String LFC_DATA_FOLDER = "data";
    private static final String LFN = "lfn";
    private static final String FILE = "file:";
    private ShellWrapper ssh;
    private RowTableModel<Job> jobs;
    private RowTableModel<LogicalFile> files;
    private ExecutorService pool;
    private List<Future> tasks;
    private Map<Object, Thread> running;
    private Map<FileListener, Class> listeners;
    private File vletBootstrapper, tempdir;
    private String user;
    private VirtualOrganisation vo;

    public Controller() {
        
        tasks = new ArrayList<>();
        running = new HashMap<>();
        listeners = new HashMap<>();

        int processors = Runtime.getRuntime().availableProcessors();
        int nThreads = processors > 1 ? processors - 1 : processors;
        pool = Executors.newFixedThreadPool(nThreads);

        for (Application application : Application.values()) {
            addFileListener(BinaryFile.class, application);
        }

        try {
            File projectDir = new File(Controller.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath()).getParentFile();
            vletBootstrapper = new File(projectDir.getPath() + "/lib/vlet-1.5.0/bin/bootstrapper.jar");
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        // Warning for missing vlet-1.5.0 folder in lib/
        BrowseOptionDialog dialog = new BrowseOptionDialog(null,
                "Missing Library Folder",
                "<html><body><p style='width: 225px;'>"
                + "Unable to find " + vletBootstrapper.getPath()
                + ".</p><br /><p style='width: 225px;'>"
                + "You won't be able to upload or download files to the grid."
                + "</p></body></html>");

        loop:
        while (!vletBootstrapper.exists()) {

            dialog.setVisible(true);
            switch (dialog.getOption()) {
                case BrowseOptionDialog.QUIT:
                    System.exit(0);
                    break loop;
                case BrowseOptionDialog.BROWSE:
                    JFileChooser fileChooser = FileChooserFactory.getSelectFolderDialog();
                    fileChooser.setDialogTitle("Select the vlet-1.5.0 installation folder");
                    if (fileChooser.showOpenDialog(null) == JOptionPane.OK_OPTION) {
                        vletBootstrapper = new File(fileChooser.getSelectedFile() + "/bin/", "bootstrapper.jar");
                    }
                    break;
                case BrowseOptionDialog.CONTINUE:
                    break loop;
            }
        }
    }

    public void setJobModel(RowTableModel<Job> jobModel) {
        this.jobs = jobModel;
    }

    public void setFileModel(RowTableModel<LogicalFile> fileModel) {
        this.files = fileModel;
    }

    public String getVirtualOrganisation() {
        return vo.toString();
    }

    public String getCatalogHomePath() {
        return "/grid/" + vo + "/" + user + "/" + LFC_DATA_FOLDER + "/";
    }

    public URI getCatalogWorkingDirectory() {
        try {
            return new URI(LFN, LFC_HOST, getCatalogHomePath(), null);
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public String getCatalogHostAddress() {
        return LFC_HOST;
    }

    public Job getJob(int row) {
        return jobs.getRow(row);
    }

    public List<Job> getJobs() {
        return jobs.getObjects();
    }

    public LogicalFile getFile(int row) {
        return files.getRow(row);
    }

    public List<LogicalFile> getFiles() {
        return files.getObjects();
    }

    public File getTemporaryDirectory() {
        return tempdir;
    }

    private File createTempDir() {
        File temp = null;
        try {
            temp = Files.createTempDirectory("emcgridclient", new FileAttribute[]{}).toFile();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return temp;
    }

    public void addJob(Thread uploader, Job job) {
        try {
            jobs.add(job);
            uploadToUI(job);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void addFile(final LogicalFile file, final File archive) {
        //add to model
        files.add(file);

        Thread task = new Thread() {
            @Override
            public void run() {
                try {
                    file.setProgress(1);
                    file.setDiskspace(archive.length());
                    uploadToSE(archive, false);
                    file.setProgress(100);
                    uploadToUI(file);
                    notifyFileAdded(file);
                } catch (InterruptedException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    file.setProgress(-1);
                }
            }
        };

        running.put(file, task);
        executeInBackground(task);
    }

    public void addFile(final LogicalFile file, final ArchiveBuilder archiver) {
        //add to model
        files.add(file);

        Thread task = new Thread() {
            @Override
            public void run() {
                try {
                    archiver.setPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals(ArchiveBuilder.PROGRESS_PROPERTY_NAME)) {
                                file.setProgress((int) evt.getNewValue() / 2);
                                notifyFileChanged(file);
                            }
                        }
                    });

                    File archive = new File(tempdir, file.getID());
                    archiver.createArchive(archive);
                    file.setProgress(50);
                    file.setDiskspace(archive.length());
                    notifyFileChanged(file);

                    uploadToSE(archive, false);

                    file.setProgress(100);
                    uploadToUI(file);
                    notifyFileChanged(file);
                    notifyFileAdded(file);
                } catch (InterruptedException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    file.setProgress(-1);
                    notifyFileChanged(file);
                }
            }
        };

        running.put(file, task);
        executeInBackground(task);
    }

    public void removeJob(Job job) {
        try {
            if (job.getProgress() < 100) {
                //cancel upload & submit
                if (running.containsKey(job)) {
                    running.get(job).interrupt();
                    running.remove(job);
                }
            } else if (!job.isSubmitted()) {
                //cancel running job
                cancelJob(job);
            }

            // remove input files
            boolean inputLocked = false;
            for (Job other : jobs.getObjects()) {
                if (other != job && other.getInput().equals(job.getInput())) {
                    inputLocked = true;
                    break;
                }
            }
            if (!inputLocked && exists(job.getInput())) {
                deleteFileFromSE(job.getInput());
            }

            // remove results
            if (exists(job.getOutput())) {
                deleteFileFromSE(job.getOutput());                              //TODO: check if not being downloaded right now
            }

            // remove prerequisites
            boolean prerequisitesLocked = false;
            for (Job other : jobs.getObjects()) {
                if (other != job && other.getPrequisites().equals(job.getPrequisites())) {
                    prerequisitesLocked = true;
                    break;
                }
            }
            if (!prerequisitesLocked && exists(job.getPrequisites())) {
                deleteFileFromSE(job.getPrequisites());
            }

            // remove wrapper
            boolean wrapperLocked = false;
            for (Job other : getJobs()) {
                if (other != job && other.getWrapper().equals(job.getWrapper())) {
                    wrapperLocked = true;
                    break;
                }
            }
            if (!wrapperLocked) {
                ssh.rm(FILES_DIR + job.getWrapper().getName());
            }

            // remove jdl
            if (ssh.test("-e", FILES_DIR + job.getJobFileName())) {
                ssh.rm(FILES_DIR + job.getJobFileName());
            }

            // remove meta file
            if (ssh.test("-e", OBJECT_DIR + job.getID() + PersistentObject.SERIALIZED_SUFFIX)) {
                ssh.rm(OBJECT_DIR + job.getID() + PersistentObject.SERIALIZED_SUFFIX);
            }

            // remove from model
            jobs.remove(job);

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public boolean removeFile(LogicalFile file) throws IOException {
        if (file.getStatus() < 100) {
            //cancel upload
            if (running.containsKey(file)) {
                running.get(file).interrupt();
                running.remove(file);
            }
        }

        if (exists(file) && !deleteFileFromSE(file.getID())) {
            return false;
        }

        ssh.rm(OBJECT_DIR + file.getID() + PersistentObject.SERIALIZED_SUFFIX);
        files.remove(file);
        notifyRecordRemoved(file);

        return true;
    }

    public void removeJobs(int... indexes) {
        // Sort on ascending order to avoid corrupt indexes
        Integer[] sorted = ArrayUtils.toObject(indexes);
        Arrays.sort(sorted, Collections.reverseOrder());
        for (int rowIndex : sorted) {
            removeJob(jobs.getRow(rowIndex));
        }
    }

    public boolean removeFiles(int... indexes) throws IOException {
        // Sort on ascending order to avoid corrupt indexes
        boolean succes = true;
        Integer[] sorted = ArrayUtils.toObject(indexes);
        Arrays.sort(sorted, Collections.reverseOrder());
        for (int rowIndex : sorted) {
            succes = succes && removeFile(files.getRow(rowIndex));
        }
        return succes;
    }

    //<editor-fold defaultstate="collapsed" desc=" Session Control Methods ">
    public boolean login(String host, VirtualOrganisation vo, String user, char[] password) throws IOException {

        this.vo = vo;
        this.user = user;

        ssh = new ShellWrapper();
        ssh.login(host, user, password);
        tempdir = createTempDir();

        if (!ssh.test("-d", PWD)) {
            ssh.mkdir(PWD);
        }
        if (!ssh.test("-d", OBJECT_DIR)) {
            ssh.mkdir(OBJECT_DIR);
        }
        if (!ssh.test("-d", FILES_DIR)) {
            ssh.mkdir(FILES_DIR);
        }
        return ssh.hasLoggedOn();
    }

    public void logout() {

        for (Future future : tasks) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }

        pool.shutdownNow();

        if (ssh != null) {
            try {
                ssh.logout();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        if (tempdir != null) {
            try {
                FileUtils.deleteDirectory(tempdir);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void setupPublicKeyAuthentication() {
    }

    public void removePublicKeyAuthentication() {
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc=" Proxy Creation Methods ">
    public boolean createLocalProxy(char[] password, int lifetime) throws IOException, InterruptedException {
        return LocalProxy.getProxy(vo).createLocalProxy(password, lifetime);
    }

    public int getLocalProxyLifetime() {
        return LocalProxy.getProxy(vo).getLocalProxyLifetime();
    }

    /**
     * Executes the voms-proxy-init program on the UI host to generate a proxy
     * with VOMS information.
     *
     * @param password private key passphrase
     * @param lifetime proxy lifetime in hours
     * @return true if succeeded, else false
     * @throws IOException
     */
    public boolean createRemoteProxy(char[] password, int lifetime) throws IOException {
        return ssh.createProxy(vo, lifetime, password);
    }

    /**
     * Returns the time in hours the proxy has left.
     *
     * @return the time in hours that the proxy has left or -1 if no proxy has
     * been found.
     */
    public int getRemoteProxyLifetime() {
        String[] output;
        try {
            output = ssh.execute("voms-proxy-info");
            for (String line : output[ShellWrapper.STDOUT].split(ShellWrapper.LINE_SEPARATOR)) {
                if (line.startsWith("timeleft")) {
                    String[] time = line.substring(line.indexOf(':') + 1).trim().split(":");
                    return Integer.parseInt(time[0]);
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return -1;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc=" Common Methods ">
    /**
     * Submits a job to the Grid Workload Management System (WMS) through the
     * glite-wms-job-submit command. Job information is stored to a file on the
     * UI host.
     *
     * @param path the name of a jdl-file
     * @return the job id, or null
     * @throws IOException
     */
    public String submitJob(String jobFileName) throws IOException, InterruptedException {
        String[] output = ssh.execute("cd " + FILES_DIR + " && glite-wms-job-submit", "--vo", getVirtualOrganisation(), "-a", jobFileName);
        for (String line : output[ShellWrapper.STDOUT].split(ShellWrapper.LINE_SEPARATOR)) {
            if (line.startsWith("https://")) {
                return line;
            }
        }
        return null;
    }

    public void cancelJob(Job job) throws IOException {
        String[] output = ssh.execute("glite-wms-job-cancel", "--vo", getVirtualOrganisation(), job.getGridID());
        if (Integer.parseInt(output[ShellWrapper.EXIT_VALUE]) != 0) {
            LOGGER.severe(output[ShellWrapper.STDERR]);
        }
    }

    public String getJobStatus(String id) throws IOException {
        String[] output = ssh.execute("glite-wms-job-status", "--vo", getVirtualOrganisation(), id);
        return output[ShellWrapper.STDOUT];
    }

    public boolean existsUI(LogicalFile file) {
        try {
            return ssh.test("-e", OBJECT_DIR + file.getID());

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Checks whether a file has been registered in the LFC.
     *
     * @param fileName the name of the file
     * @return true if the path exists, else false
     * @throws IOException
     */
    public boolean exists(LogicalFile file) {
        return exists(file.getID());
    }

    public boolean exists(String fileName) {
        URI uri = getCatalogWorkingDirectory();
        String path = uri.getHost() + ":" + uri.getPath() + fileName;
        String[] output;
        try {
            output = ssh.execute("lfc-ls", path);
            return Integer.parseInt(output[ShellWrapper.EXIT_VALUE]) == 0;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return false;
    }

    public void uploadToUI(PersistentObject object) throws IOException {
        File file = new File(tempdir, object.getID() + PersistentObject.SERIALIZED_SUFFIX);
        object.saveToFile(file);
        ssh.upload(file, OBJECT_DIR);
    }

    public void uploadToUI(File file) throws IOException {
        if (!ssh.test("-e", FILES_DIR + file.getName())) {
            ssh.upload(file, FILES_DIR);
        }
    }

    public void uploadToSE(File file, boolean overwrite) throws IOException, InterruptedException {

        if (!vletBootstrapper.exists()) {
            throw new IOException("Error: Unable to access file " + vletBootstrapper.getPath());
        }

        if (overwrite || !exists(file.getName())) {

            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(vletBootstrapper.getPath());
            command.add("nl.uva.vlet.vrs.tools.URICopy");
            command.add("-v");
            command.add("-mkdirs");
            //command.add("-force");
            command.add(file.getAbsolutePath());
            command.add(getCatalogWorkingDirectory().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            LOGGER.log(Level.INFO, "Executing: {0}", command.toString().replace(",", "").replace("[", "").replace("]", ""));

            Process process = pb.start();
            process.waitFor();

            if (process.exitValue() != 0) {
                LOGGER.severe(process.getInputStream().toString());
            }
        } else {
            LOGGER.log(Level.INFO, "Skipping upload of {0}, LFC entry already exists!", file.getName());
        }
    }

    public boolean downloadFromSE(LogicalFile file, File destination) throws IOException, InterruptedException {
        
        if (!vletBootstrapper.exists()) {
            throw new IOException("Error: Unable to access file " + vletBootstrapper.getPath());
        }
        
        String path = getCatalogWorkingDirectory().toString() + file.getID();

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(vletBootstrapper.getPath());
        command.add("nl.uva.vlet.vrs.tools.URICopy");
        command.add("-v");
        command.add("-mkdirs");
        command.add("-force");
        command.add(path);
        command.add(FILE + destination.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        LOGGER.log(Level.INFO, "Executing: {0}", command.toString().replace(",", "").replace("[", "").replace("]", ""));

        Process process = pb.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            LOGGER.severe(process.getInputStream().toString());
        }

        return process.exitValue() == 0;
    }

    public boolean deleteFileFromSE(LogicalFile file) throws IOException {
        return deleteFileFromSE(file.getID());
    }

    public boolean deleteFileFromSE(String filename) throws IOException {
        URI uri = getCatalogWorkingDirectory();
        String path = uri.getScheme() + ":" + uri.getPath() + filename;
        String[] output = ssh.execute("lcg-del", "--vo", getVirtualOrganisation(), "-a", path);
        if (Integer.parseInt(output[ShellWrapper.EXIT_VALUE]) == 0) {
            return true;
        }
        LOGGER.log(Level.SEVERE, "Unable to delete file {0}", output[ShellWrapper.STDERR]);
        return false;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc=" Background Tasks ">
    public void executeInBackground(Runnable... runnables) {
        for (Runnable task : runnables) {
            tasks.add(pool.submit(task));
        }
    }

    public boolean finishedAllTasks() {
        for (Future future : tasks) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    public void fetchData() {
        executeInBackground(new SwingWorker<Void, PersistentObject>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    for (String filepath : ssh.ls(OBJECT_DIR + "*" + PersistentObject.SERIALIZED_SUFFIX)) {

                        FileInputStream fi = null;
                        ObjectInputStream in = null;
                        try {
                            String filename = filepath.substring(filepath.lastIndexOf("/"));
                            File temp = new File(tempdir, filename);
                            ssh.download(filepath, temp);
                            fi = new FileInputStream(temp);
                            in = new ObjectInputStream(fi);
                            publish((PersistentObject) in.readObject());
                        } catch (ClassNotFoundException | ClassCastException | IOException ex) {
                            LOGGER.log(Level.SEVERE, "Skipped fetching file {0}: {1}", new Object[]{filepath, ex.getMessage()});
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                            if (fi != null) {
                                fi.close();
                            }
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
                return null;
            }

            @Override
            protected void process(List<PersistentObject> chunks) {
                for (PersistentObject object : chunks) {
                    if (object instanceof Job) {
                        Job job = (Job) object;
                        if (jobs.contains(job)) {
                            jobs.replace(job);
                        } else {
                            jobs.add(job);
                        }
                    } else if (object instanceof LogicalFile) {
                        LogicalFile file = (LogicalFile) object;
                        if (files.contains(file)) {
                            files.replace(file);
                        } else {
                            files.add(file);
                        }
                        notifyFileAdded(file);
                    }
                }
            }
        });
    }

    public void updateJobStats() {
        executeInBackground(new Runnable() {
            @Override
            public void run() {
                for (Job job : jobs.getObjects()) {
                    try {
                        String status = job.getStatus();
                        String prefix = "Current Status:";
                        if (job.isSubmitted()) {

                            String stats = getJobStatus(job.getGridID());
                            for (String line : stats.split(ShellWrapper.LINE_SEPARATOR)) {
                                line = line.trim();

                                if (line.startsWith(prefix)) {
                                    status = line.substring(prefix.length()).trim();
                                }
                            }
                        } else {
                            status = "Uploading";
                        }

                        if (!job.getStatus().equals(status)) {
                            job.setStatus(status);
                            notifyJobChanged(job);
                            uploadToUI(job);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
            }
        });
    }

    public void showMessageOnLatchRelease(final CountDownLatch latch, final String title, final String message) {
        executeInBackground(new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                latch.await();
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    //</editor-fold>

    public final void addFileListener(Class<?> c, FileListener listener) {
        listeners.put(listener, c);
    }

    public void removeFileCatalogListener(FileListener listener) {
        listeners.remove(listener);
    }

    public void notifyJobChanged(Job job) {
        int index = jobs.indexOf(job);
        jobs.fireTableRowsUpdated(index, index);
    }

    public void notifyFileChanged(LogicalFile file) {
        int index = files.indexOf(file);
        files.fireTableRowsUpdated(index, index);
    }

    private void notifyFileAdded(LogicalFile record) {
        for (FileListener l : listeners.keySet()) {
            if (listeners.get(l).isInstance(record)) {
                l.fileAdded(record);
            }
        }
    }

    private void notifyRecordRemoved(LogicalFile record) {
        for (FileListener l : listeners.keySet()) {
            if (listeners.get(l).isInstance(record)) {
                l.fileDeleted(record);
            }
        }
    }
}
