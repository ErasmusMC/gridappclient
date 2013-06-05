/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import java.io.File;

/**
 *
 * @author bram
 */
public enum FileType {

    ANNOTATION("Annotation", "gtf", "gff", "gff3"),
    BOWTIE_INDEX("Bowtie Index", "bt2", "ebwt"),
    FASTA("Sequence File", "fa", "fasta"),
    FASTQ("Sequence File", "fq", "fastq"),
    FILE("File"),
    FOLDER("Folder"),
    JUNCTION("Junction File", "juncs"),
    MAPPED_SEQUENCE("Mapped Sequence", "bam", "sam");
    
    private String name;
    private String[] extensions;
    private FileType(String name, String... extensions) {
        this.name = name;
        this.extensions = extensions;
    }
    
    public String[] getFileExtensions() {
        return extensions;
    }
    
    public static FileType parse(File file) {
        return parse(file.getName());
    }

    public static FileType parse(String filename) {
        if (filename.contains(".")) {
            return parseExtension(filename.substring(filename.lastIndexOf(".")));
        }
        return FILE;
    }

    private static FileType parseExtension(String extension) {

        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }

        for (FileType type : values()) {
            for (String ext : type.extensions) {
                if (extension.equals(ext)) {
                    return type;
                }
            }
        }
        return FILE;
    }

    @Override
    public String toString() {
        return name;
    }
}
