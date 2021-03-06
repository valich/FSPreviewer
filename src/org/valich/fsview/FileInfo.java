package org.valich.fsview;

import org.apache.commons.net.ftp.FTPFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.valich.fsview.FileInfo.FileAttribute.*;

/**
 * Immutable class, containing file info, such as name, type and size.
 */
public final class FileInfo {
    public enum FileAttribute {
        IS_REGULAR_FILE,
        IS_DIRECTORY,
        IS_SYMLINK
    }

    private final String name;
    private final EnumSet<FileAttribute> attributes;
    private long size;

    @NotNull
    public static final FileInfo UP_DIR = new FileInfo("..");
    static {
        UP_DIR.setAttribute(IS_DIRECTORY, true);
    }

    private FileInfo(String name) {
        if (name == null)
            name = "";

        this.name = name;
        this.attributes = EnumSet.noneOf(FileAttribute.class);
        this.size = -1;
    }

    private void setAttribute(@NotNull FileAttribute attr, boolean setTrue) {
        if (setTrue)
            attributes.add(attr);
        else
            attributes.remove(attr);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public EnumSet<FileAttribute> getAttributes() {
        return attributes;
    }

    public long getSize() {
        return size;
    }

    @Override public String toString() {
        return String.format("File:{name=%s, size=%d, attributes=%s}", name, size, attributes);
    }

    @NotNull
    private static String getClearedName(@NotNull Path file) {
        final String delim = file.getFileSystem().getSeparator();

        if (file.getFileName().toString().contains(delim)) {
            String cleared = file.getFileName().toString().replace(delim, "");
            if (file.getFileName().endsWith(cleared))
                return cleared;
        }
        return file.getFileName().toString();
    }

    @NotNull
    public static FileInfo valueOf(@NotNull Path file) throws IOException {
//        if (!Files.exists(file))
//            throw new NoSuchFileException("No such file");

        FileInfo result = new FileInfo(getClearedName(file));
        result.setAttribute(IS_DIRECTORY, Files.isDirectory(file));
        result.setAttribute(IS_REGULAR_FILE, Files.isRegularFile(file));
        result.setAttribute(IS_SYMLINK, Files.isSymbolicLink(file));
        result.size = Files.size(file);

        return result;
    }

    @NotNull
    public static FileInfo valueOf(@NotNull FTPFile file) {
        FileInfo result = new FileInfo(file.getName());
        result.setAttribute(IS_DIRECTORY, file.isDirectory());
        result.setAttribute(IS_REGULAR_FILE, file.isFile());
        result.setAttribute(IS_SYMLINK, file.isSymbolicLink());
        result.size = file.getSize();

        return result;
    }

    @Override public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof FileInfo))
            return false;

        FileInfo other = (FileInfo) o;
        return this.getName().equals(other.getName()) &&
                this.getAttributes().equals(other.getAttributes()) &&
                this.getSize() == other.getSize();
    }

    @Override public int hashCode() {
        return this.getName().hashCode();
    }
}
