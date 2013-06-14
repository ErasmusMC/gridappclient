/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.control;

import au.org.arcs.auth.swiss_proxy_knife.SwissProxyKnife;
import client.model.VirtualOrganisation;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.common.StreamCopier;

/**
 * Utility class to check upon a local proxy certificate.
 *
 * @author bram
 */
public class LocalProxy {

    private static final String VOMS_SERVER = "voms.grid.sara.nl";
    private static final int PORT = 30018;
    private static final String VO_ISSUER = "/O=dutchgrid/O=hosts/OU=sara.nl/CN=voms.grid.sara.nl";
    private static final Logger LOGGER = Logger.getLogger(LocalProxy.class.getSimpleName());
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private VirtualOrganisation vo;
    private File vomsesFile;
    private File jarFile;

    private LocalProxy(VirtualOrganisation vo) {
        this.vo = vo;
        
        String os = System.getProperty("os.name");
        String home = System.getProperty("user.home");
        try {
            jarFile = new File(SwissProxyKnife.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        if (os.contains("Windows")) {
            vomsesFile = new File(home + "/glite/vomses/" + vo + "-voms.grid.sara.nl");
            File dir = vomsesFile.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + dir);
            }

            try {
                Files.setAttribute(dir.getParentFile().toPath(), "dos:hidden", true);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else {
            vomsesFile = new File(home + "/.glite/vomses/" + vo + "-voms.grid.sara.nl");
        }
    }

    public static LocalProxy getProxy(VirtualOrganisation vo) {
        return new LocalProxy(vo);
    }

    public int getLocalProxyLifetime() {
        try {
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(jarFile.getPath());
            command.add("-m");
            command.add("load-local-proxy");
            command.add("-a");
            command.add("info");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            LOGGER.log(Level.INFO, "Executing: {0}", command.toString().replace(",", "").replace("[", "").replace("]", ""));

            Process process = pb.start();
            process.waitFor();

            if (process.exitValue() == 0) {
                String output = IOUtils.readFully(process.getInputStream()).toString();
                for (String line : output.split(LINE_SEPARATOR)) {
                    if (line.startsWith("timeleft")) {
                        String[] time = line.substring(line.indexOf(':') + 1).trim().split(",");
                        return Integer.parseInt(time[0].substring(0, time[0].indexOf('h')));
                    }
                }
            }
            LOGGER.severe(IOUtils.readFully(process.getInputStream()).toString());

        } catch (InterruptedException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return -1;
    }

    public boolean createLocalProxy(char[] password, int lifetime) throws IOException, InterruptedException {

        if (lifetime <= 0) {
            throw new IllegalArgumentException("Proxy-lifetime must be > 0");
        }

        if (!vomsesFile.exists()) {
            createVomsFile();
        }

        BufferedWriter bw = null;
        try {
            LOGGER.log(Level.INFO, "Will locally exec `java -jar \"{0}\" -m grid-proxy-init -v /{1} -l {2} -a store-local`",
                    new Object[]{jarFile.getAbsolutePath(), vo, lifetime});

            Process process = Runtime.getRuntime().exec("java -jar \"" + jarFile.getAbsolutePath()
                    + "\" -m grid-proxy-init -v /" + vo + " -l " + lifetime + " -a store-local");

            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            new StreamCopier(process.getErrorStream(), stderr).spawnDaemon("stderr-copier");
            new StreamCopier(process.getInputStream(), stdout).spawnDaemon("stdout-copier");

            final String question = "Please provide your private key passphrase:";
            String line;
            while (true) {

                if (!stderr.toString().isEmpty()) {
                    break;
                }

                line = stdout.toString();

                if (!line.equals(question)) {
                    bw.newLine();
                    break;
                }

                if (stdout.toString().equals(question)) {
                    bw.write(password);
                    bw.newLine();
                    bw.flush();
                    break;
                }
            }

            if (process.waitFor() != 0) {
                throw new IOException(stderr.toString());
            }

            LOGGER.info(stdout.toString().split(LINE_SEPARATOR)[1]);
            return true;

        } finally {
            if (bw != null) {
                bw.close();
            }
        }
    }

    private void createVomsFile() throws IOException {
        BufferedWriter writer = null;
        try {
            vomsesFile.createNewFile();
            writer = new BufferedWriter(new FileWriter(vomsesFile));
            StringBuilder info = new StringBuilder();
            info.append("\"").append(vo).append("\" \"")
                    .append(VOMS_SERVER).append("\" \"")
                    .append(PORT).append("\" \"")
                    .append(VO_ISSUER).append("\" \"")
                    .append(vo).append("\"");
            writer.write(info.toString());

            LOGGER.log(Level.INFO, "Created file: {0}", vomsesFile.getAbsoluteFile());

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
