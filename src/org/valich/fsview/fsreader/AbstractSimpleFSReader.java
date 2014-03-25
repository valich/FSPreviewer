package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.*;

/**
 * Created by valich on 20.03.14.
 */
abstract class AbstractSimpleFSReader implements SimpleFSReader {
    private final Path root;
    private Path path;

    @NotNull
    @Override public synchronized Path getWorkingDirectory() {
        return path;
    }

    @Override public synchronized boolean changeDirectory(@NotNull Path path) {
        assert this.path.isAbsolute();

        Path resolved = PathHelper.resolveWithoutGoingDown(this.path, path);
        if (resolved == null)
            return false;

        this.path = resolved.normalize();
        return true;
    }


    protected AbstractSimpleFSReader(FileSystem fs) {
        path = fs.getRootDirectories().iterator().next();
        root = path.getRoot();
    }

    protected Path getPath() {
        return path;
    }
}
