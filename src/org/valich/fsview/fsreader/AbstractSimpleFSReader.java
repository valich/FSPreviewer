package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

abstract class AbstractSimpleFSReader implements SimpleFSReader {
    private Path path;

    @NotNull
    @Override
    public synchronized Path getWorkingDirectory() {
        return path;
    }

    @Override
    public synchronized boolean changeDirectory(@NotNull Path path) throws IOException {
        assert this.path.isAbsolute();

        Path resolved = PathHelper.resolveWithoutGoingDown(this.path, path);
        if (resolved == null)
            return false;

        this.path = resolved.normalize();
        return true;
    }


    protected AbstractSimpleFSReader(FileSystem fs) {
        path = fs.getRootDirectories().iterator().next();
    }

    protected Path getPath() {
        return path;
    }

    protected void setPath(Path path) {
        this.path = path;
    }
}
