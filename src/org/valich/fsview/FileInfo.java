package org.valich.fsview;

import org.apache.commons.net.ftp.FTPFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import static org.valich.fsview.FileInfo.FileAttribute.*;

/**
 * Created by valich on 21.03.14.
 */
public final class FileInfo {
    public enum FileAttribute {
        IS_REGULAR_FILE,
        IS_DIRECTORY,
        IS_SYMLINK
    }

    private String name;
    private EnumSet<FileAttribute> attributes;
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

    @Nullable
    public static FileInfo valueOf(File file) {
        if (file == null)
            return null;

        FileInfo result = new FileInfo(file.getName());
        result.setAttribute(IS_DIRECTORY, file.isDirectory());
        result.setAttribute(IS_REGULAR_FILE, file.isFile());
        result.size = file.length();

        return result;
    }

    @Nullable
    public static FileInfo valueOf(Path file) {
        if (file == null)
            return null;
        if (!Files.exists(file))
            return null;

        FileInfo result = new FileInfo(file.getFileName().toString());
        result.setAttribute(IS_DIRECTORY, Files.isDirectory(file));
        result.setAttribute(IS_REGULAR_FILE, Files.isRegularFile(file));
        result.setAttribute(IS_SYMLINK, Files.isSymbolicLink(file));
        try {
            result.size = Files.size(file);
        } catch (IOException e) {
//            e.printStackTrace();
            result.size = -1;
        }

        return result;
    }

    @Nullable
    public static FileInfo valueOf(FTPFile file) {
        if (file == null)
            return null;

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
        return this.getName().equals(((FileInfo) o).getName());
    }

    @Override public int hashCode() {
        return this.getName().hashCode();
    }
}
