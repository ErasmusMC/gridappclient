/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Utility class for matching files to certain types of data within.
 *
 * @author bram
 */
public enum FileChooser {

    ANNOTATION("Annotation", "gtf", "gff", "gff3"),
    BOWTIE_INDEX("Index File", "bt2", "ebwt"),
    FASTA("Raw Sequence", "fa"),
    FASTQ("Raw Sequence", "fastq", "fq"),
    FILE("File"),
    JUNCTION("Junction", ".juncs");
    private String displayName;
    private List<String> extensions;
    private JFileChooser fileChooser;
    private static final String ADD_BUTTON_TEXT = "Add File";
    private static final String ADD_DIALOG_TITLE = "Select your file to upload";
    private static final String ADD_BUTTON_TEXT_PLURAL = "Add Files";
    private static final String ADD_DIALOG_TITLE_PLURAL = "Select your files to upload";
    private static final String SAVE_BUTTON_TEXT = "Save File";
    private static final String SAVE_DIALOG_TITLE = "Select your folder to save to";

    private FileChooser(String name, String... extensions) {
        this.displayName = name;
        this.extensions = Arrays.asList(extensions);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getExtensions() {
        return extensions.toArray(new String[]{});
    }

    public JFileChooser getAddFileDialog() {
        return getAddFileDialog(false);
    }

    public JFileChooser getAddFileDialog(boolean multiple) {

        if (fileChooser == null) {
            fileChooser = new JFileChooser();

            if (getExtensions() != null && getExtensions().length > 0) {
                fileChooser.setFileFilter(new FileNameExtensionFilter(getDisplayName(), getExtensions()));
                fileChooser.setAcceptAllFileFilterUsed(false);
            }
        }

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(multiple);
        fileChooser.setDialogTitle(multiple ? ADD_DIALOG_TITLE_PLURAL : ADD_DIALOG_TITLE);
        fileChooser.setApproveButtonText(multiple ? ADD_BUTTON_TEXT_PLURAL : ADD_BUTTON_TEXT);

        return fileChooser;
    }

    public JFileChooser getSaveFileDialog() {

        if (fileChooser == null) {
            fileChooser = new JFileChooser();
        }

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setDialogTitle(SAVE_DIALOG_TITLE);
        fileChooser.setApproveButtonText(SAVE_BUTTON_TEXT);
        return fileChooser;
    }

    public static FileChooser parse(File file) {
        return parse(file.getName());
    }

    public static FileChooser parse(String filename) {
        if(filename.contains(".")) {
            return parseExtension(filename.substring(filename.lastIndexOf(".")));
        }
        return FILE;
    }

    private static FileChooser parseExtension(String extension) {

        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }

        for (FileChooser type : values()) {
            for (String ext : type.extensions) {
                if (extension.equals(ext)) {
                    return type;
                }
            }
        }
        return FILE;
    }
}
