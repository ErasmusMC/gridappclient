/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author bram
 */
public class Job extends PersistentObject {

    private static final String JDL_SUFFIX = ".jdl";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss MMM dd yyyy");
    private String id, batchID, status;
    private Map<String, String> options;
    private Date date;
    private Application application;
    private BinaryFile binaries;
    private LogicalFile input;
    private LogicalFile prerequisites;
    private LogicalFile output;
    private boolean success;
    private int progress;

    public Job(Batch batch, String localID, Map<String, String> options) {
        super(batch.id + "_" + localID);

        this.input = new LogicalFile(localID);
        this.output = new LogicalFile(getID() + "_" + batch.getApplication()
                .toString().toLowerCase() + "_out");
        this.date = new Date();
        this.status = "Uploading";
        this.success = false;

        this.batchID = batch.id;
        this.application = batch.getApplication();
        this.binaries = batch.getBinaries();
        this.prerequisites = new LogicalFile(batch.getPrequisiteFileName());

        this.options = options;
    }

    public boolean isSubmitted() {
        return id != null;
    }

    public boolean isFinished() {
        return success;
    }

    public String getGridID() {
        return id;
    }

    public String getBatch() {
        return batchID;
    }

    public String getJobFileName() {
        return getID() + JDL_SUFFIX;
    }

    public String getWrapperName() {
        return application.getWrapperName();
    }

    public LogicalFile getInput() {
        return input;
    }

    public LogicalFile getOutput() {
        return output;
    }

    public LogicalFile getPrequisites() {
        return prerequisites;
    }

    public BinaryFile getApplication() {
        return binaries;
    }

    public String getDateCreated() {
        return DATE_FORMAT.format(date);
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setStatus(String status) {
        
        if(status.equals("Done (Success)")) {
            success = true;
        }
        this.status = status;
    }

    public void setId(String id) {
        this.id = id;
    }
}
