/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.control;

import client.model.GridUserInterface;
import client.model.VirtualOrganisation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPDownloadClient;
import net.schmizz.sshj.xfer.scp.SCPUploadClient;

/**
 * Utility class containing simplified methods of frequently used programs.
 *
 * @author bram
 */
public class ShellWrapper {

    public static final int EXIT_VALUE = 0, STDOUT = 1, STDERR = 2;
    public static final String LINE_SEPARATOR = "\n";
    private static final Logger LOGGER = Logger.getLogger(ShellWrapper.class.getSimpleName());
    private SSHClient ssh;

    public ShellWrapper() {
        ssh = new SSHClient();
        for (GridUserInterface host : GridUserInterface.values()) {
            ssh.addHostKeyVerifier(host.getFingerprint());
        }
    }

    public void login(String host, String user, char[] password) throws IOException {
        ssh.connect(host);
        ssh.authPassword(user, password);

        for (int i = 0; i < password.length; i++) {
            password[i] = 0;
        }
    }

    public boolean hasLoggedOn() {
        return ssh.isConnected() && ssh.isAuthenticated();
    }

    public void logout() throws IOException {
        ssh.disconnect();
    }

    public String[] execute(String program, String... arguments) throws IOException {
        Session session = ssh.startSession();
        try {
            Session.Command command = session.exec(
                    new CommandBuilder(program, arguments).toString());
            String stdout = IOUtils.readFully(command.getInputStream()).toString();
            String stderr = IOUtils.readFully(command.getErrorStream()).toString();
            command.join(2, TimeUnit.SECONDS);
            return new String[]{
                        String.valueOf(command.getExitStatus()),
                        stdout, stderr};
        } finally {
            session.close();
        }
    }

    class CommandBuilder {

        private final String command;

        public CommandBuilder(String program, String... arguments) {
            StringBuilder builder = new StringBuilder();
            builder.append(program);
            for (String argument : arguments) {
                builder.append(" ");
                builder.append(argument);
            }
            command = builder.toString();
        }

        @Override
        public String toString() {
            return command;
        }
    }

    public List<String> ls(String filepath) {
        String[] output;
        try {
            output = execute("ls", filepath);
            if (Integer.parseInt(output[EXIT_VALUE]) == 0) {
                List<String> files = new ArrayList<>();
                for(String filename : output[STDOUT].split(LINE_SEPARATOR)) {
                    if(!filename.isEmpty()) {
                        files.add(filename);
                    }
                }
                return files;
            }
            Logger.getLogger(ShellWrapper.class.getName()).log(Level.WARNING, output[2]);
        } catch (IOException ex) {
            Logger.getLogger(ShellWrapper.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        return new ArrayList<>();
    }

    public boolean test(String... arguments) throws IOException {
        String[] output = execute("test", arguments);
        return Integer.parseInt(output[EXIT_VALUE]) == 0;
    }

    public void mkdir(String dirpath) throws IOException {
        SFTPClient sftp = ssh.newSFTPClient();
        try {
            sftp.mkdir(dirpath);
        } finally {
            sftp.close();
        }
    }

    public boolean rmdir(String dirpath) throws IOException {
        String[] output = execute("rm", "-r", dirpath);
        return Integer.parseInt(output[EXIT_VALUE]) == 0;
    }

    /**
     * Copies (SCP) a file from the UI host.
     *
     * @param source the remote file to copy
     * @param destination the local file to copy to
     * @return true if succeeded
     * @throws IOException
     */
    public boolean download(String source, File destination) throws IOException {
        SCPDownloadClient scp = ssh.newSCPFileTransfer().newSCPDownloadClient();
        int value = scp.copy(source, new FileSystemFile(destination));
        return value == 0;
    }

    /**
     * Copies (SCP) a file to the UI host.
     *
     * @param files the local file to copy
     * @param destination the remote destination to copy to
     * @return true if succeeded
     * @throws IOException
     */
    public boolean upload(File source, String destination) throws IOException {
        SCPUploadClient scp = ssh.newSCPFileTransfer().newSCPUploadClient();
        int exitValue = scp.copy(new FileSystemFile(source.getAbsolutePath()), destination + source.getName());
        return exitValue == 0;
    }

    public boolean rm(String filepath) throws IOException {
        String[] output = execute("rm", filepath);
        return Integer.parseInt(output[EXIT_VALUE]) == 0;
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
    public boolean createProxy(VirtualOrganisation vo, int lifetime, char[] password) throws IOException {

        if (lifetime <= 0) {
            throw new IllegalArgumentException("Proxy-lifetime must be > 0");
        }
        boolean succes = false;
        Session session = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            session = ssh.startSession();
            Session.Command command = session.exec("voms-proxy-init --voms " + vo + " --valid " + lifetime + ":00");
            br = new BufferedReader(new InputStreamReader(command.getErrorStream()));
            bw = new BufferedWriter(new OutputStreamWriter(command.getOutputStream()));

            final String question = "Enter GRID pass phrase:";
            StringBuilder stderr = new StringBuilder();
            while (true) {

                stderr.append((char) br.read());

                if (!stderr.toString().equals(question.substring(0, stderr.length()))) {
                    break;
                }

                if (stderr.toString().equals(question)) {
                    bw.write(password);
                    bw.newLine();
                    bw.flush();
                    break;
                }
            }

            for (int i = 0; i < password.length; i++) {
                password[i] = 0;
            }

            String errorMessage = IOUtils.readFully(command.getErrorStream()).toString();
            command.join(2, TimeUnit.SECONDS);

            if (command.getExitStatus() != 0) {
                LOGGER.warning(errorMessage);
            } else {
                succes = true;
            }
        } finally {
            if (br != null) {
                br.close();
            }
            if (bw != null) {
                bw.close();
            }
            if (session != null) {
                session.close();
            }
        }
        return succes;
    }

    /**
     * Executes following command on the grid ui machine: lcg-del -a
     * lfn:/path/file .
     *
     * @param file the LFC file
     * @return
     */
    public boolean deleteFromLFC(String path) {
        Session session = null;
        try {
            session = ssh.startSession();
            Session.Command command = session.exec("lcg-del -a " + path);
            command.join(3, TimeUnit.SECONDS);

            if (command.getExitStatus() != 0) {
                String error = IOUtils.readFully(command.getErrorStream()).toString();
                LOGGER.log(Level.WARNING, error);
            }
            return command.getExitStatus() == 0;

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            try {
                session.close();
            } catch (TransportException | ConnectionException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return false;
    }

    /**
     * Checks whether a LFC path exists.
     *
     * @param lfc the absolute lfc path
     * @return true if the path exists, else false
     * @throws IOException
     */
    public boolean logicalFileExist(String path) {
        Session session = null;
        try {
            session = ssh.startSession();
            Session.Command command = session.exec("lfc-ls " + path);
            command.join(2, TimeUnit.SECONDS);
            return command.getExitStatus() == 0;

        } catch (TransportException | ConnectionException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            try {
                session.close();
            } catch (TransportException | ConnectionException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return false;
    }
}
