/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import static client.model.JobDescription.Attribute.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which can hold properties and can be stored to a file. This file is in
 * the Job Description Language (JDL) format.
 *
 * @author bram
 */
public class JobDescription {

    public enum Attribute {

        JobType,
        VirtualOrganisation,
        Executable,
        Arguments,
        StdInput,
        StdOutput,
        StdError,
        InputSandbox,
        OutputSandbox,
        Requirements,
        SMPGranularity,
        CPUNumber,
        RetryCount;
    }
    private static final Logger LOGGER = Logger.getLogger(JobDescription.class.getSimpleName());
    protected Map<Attribute, String> properties;

    public JobDescription() {
        properties = new TreeMap<>();
    }

    /**
     * Convenience factory method to create a normal job.
     *
     * @return
     */
    public static JobDescription createNormal(Application application, String vo) {
        JobDescription jobFile = new JobDescription();
        jobFile.set(JobType, "normal");
        jobFile.set(StdError, "stderr.log");
        jobFile.set(StdOutput, "stdout.log");
        jobFile.set(OutputSandbox, "stderr.log", "stdout.log");
        jobFile.set(RetryCount, 0);
        jobFile.set(Executable, "/bin/bash");
        jobFile.set(InputSandbox, application.getWrapperName());
        //note that requirements have different synthax
        jobFile.set(Requirements, "(other.GlueHostArchitectureSMPSize >= " + application.getNumberOfThreads()
                + ") && (other.GlueCEPolicyMaxWallClockTime >= " + application.getMaximumRuntime() + ")");
        jobFile.set(SMPGranularity, application.getNumberOfThreads());
        jobFile.set(CPUNumber, application.getNumberOfThreads());
        jobFile.set(VirtualOrganisation, vo);
        return jobFile;
    }

    public final void set(Attribute attribute, int value) {
        properties.put(attribute, String.valueOf(value));
    }

    public final void set(Attribute attribute, String value) {

        switch (attribute) {
            case Requirements:
                properties.put(attribute, value);
                break;
            default:
                properties.put(attribute, "\"" + value + "\"");
        }

    }

    public final void set(Attribute attribute, String... values) {
        set(attribute, Arrays.asList(values));
    }

    public final void set(Attribute attribute, Collection<String> values) {
        StringBuilder arguments = new StringBuilder();
        switch (attribute) {
            case Arguments:
                for (Iterator<String> it = values.iterator(); it.hasNext();) {
                    String arg = it.next();
                    arguments.append(arg);
                    if (it.hasNext()) {
                        arguments.append(" ");
                    }
                }
                set(attribute, arguments.toString());
                break;
            case InputSandbox:
            case OutputSandbox:
                arguments.append("{");
                Iterator<String> iterator = values.iterator();
                while (iterator.hasNext()) {
                    arguments.append("\"").append(iterator.next()).append("\"");
                    if (iterator.hasNext()) {
                        arguments.append(",");
                    }
                }
                arguments.append("}");
                properties.put(attribute, arguments.toString());
                break;
        }
    }

    public void saveToFile(File file) {

        if (!properties.containsKey(Executable)) {
            LOGGER.info("Cannot create JobFile, because executable is null!");
            return;
        } else if (!properties.containsKey(VirtualOrganisation)) {
            LOGGER.info("Cannot create JobFile, because virtual organisation is null!");
            return;
        } else if (!properties.containsKey(Arguments)) {
            LOGGER.warning("No arguments set in the job file!");
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            synchronized (this) {
                for (Attribute attribute : properties.keySet()) {
                    bw.write(attribute + "=" + properties.get(attribute) + ";");
                    bw.newLine();
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }

            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
