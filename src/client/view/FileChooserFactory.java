/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.model.FileType;
import java.util.EnumMap;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import static client.model.FileType.*;

/**
 * Utility class to get JFileChooser instances.
 *
 * @author bram
 */
public final class FileChooserFactory {

    private static EnumMap<FileType, JFileChooser> single = new EnumMap<>(FileType.class);
    private static EnumMap<FileType, JFileChooser> multiple = new EnumMap<>(FileType.class);

    private FileChooserFactory() {
    }

    private static JFileChooser createFilteredFileChooser(FileFilter filter, boolean multiple) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(multiple);
        chooser.setDialogTitle(multiple ? "Select Files" : "Select a File");
        chooser.setApproveButtonText(multiple ? "Add Files" : "Add File");
        return chooser;
    }

    public static JFileChooser getAddAnnotationFileDialog() {
        if (!single.containsKey(ANNOTATION)) {
            FileFilter filter = new FileNameExtensionFilter("Annotation Files", ANNOTATION.getFileExtensions());
            single.put(ANNOTATION, createFilteredFileChooser(filter, false));
        }
        return single.get(ANNOTATION);
    }

    public static JFileChooser getAddBowtieIndexFilesDialog() {
        if (!multiple.containsKey(BOWTIE_INDEX)) {
            FileFilter filter = new FileNameExtensionFilter("Bowtie Index Files", BOWTIE_INDEX.getFileExtensions());
            multiple.put(BOWTIE_INDEX, createFilteredFileChooser(filter, true));
        }
        return multiple.get(BOWTIE_INDEX);
    }

    public static JFileChooser getAddFastaFileDialog() {
        if (!single.containsKey(FASTA)) {
            FileFilter filter = new FileNameExtensionFilter("Fasta Sequence Files", FASTA.getFileExtensions());
            single.put(FASTA, createFilteredFileChooser(filter, false));
        }
        return single.get(FASTA);
    }

    public static JFileChooser getAddFastqFilesDialog() {
        if (!multiple.containsKey(FileType.FASTQ)) {
            FileFilter filter = new FileNameExtensionFilter("Fastq Sequence Files", FASTQ.getFileExtensions());
            multiple.put(FileType.FASTQ, createFilteredFileChooser(filter, true));
        }
        return multiple.get(FileType.FASTQ);
    }

    public static JFileChooser getAddJunctionFileDialog() {
        if (!single.containsKey(FileType.JUNCTION)) {
            FileFilter filter = new FileNameExtensionFilter("Junction Files", JUNCTION.getFileExtensions());
            single.put(FileType.JUNCTION, createFilteredFileChooser(filter, false));
        }
        return single.get(FileType.JUNCTION);
    }

    public static JFileChooser getAddFileDialog() {
        if (!single.containsKey(FileType.FILE)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle("Select a File");
            chooser.setApproveButtonText("Add File");
            single.put(FileType.FILE, chooser);
        }
        return single.get(FileType.FILE);
    }

    public static JFileChooser getAddFilesDialog() {
        if (!multiple.containsKey(FileType.FILE)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(true);
            chooser.setDialogTitle("Select Files");
            chooser.setApproveButtonText("Add File");
            multiple.put(FileType.FILE, chooser);
        }
        return multiple.get(FileType.FILE);
    }

    public static JFileChooser getSaveToFolderDialog() {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Select a Folder");
        chooser.setApproveButtonText("Save to Folder");
        return chooser;
    }

    public static JFileChooser getSelectFolderDialog() {
        if (!single.containsKey(FileType.FOLDER)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle("Select a Folder");
            chooser.setApproveButtonText("Select Folder");
            single.put(FileType.FOLDER, chooser);
        }
        return single.get(FileType.FOLDER);
    }
}
