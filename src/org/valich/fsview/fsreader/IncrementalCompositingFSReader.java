package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valich.fsview.FileInfo;

import javax.activation.UnsupportedDataTypeException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Main implementation of FSReader suitable for use from common clients
 * All paths in queries are strings; A path with intervals of different
 * FS is possible (e.g. ftp://####/dir/file.zip/other_dir/)
 */
public final class IncrementalCompositingFSReader implements FSReader<String> {
    private Stack<SimpleFSReader> readerStack = new Stack<>();
    private Stack<Path> pathPartsStack = new Stack<>();
    private String baseUri;

    public IncrementalCompositingFSReader() {
        String baseDir = FileSystems.getDefault().getRootDirectories().iterator().next().toString();
        try {
            readerStack.add(SimpleFSReaderFactory.INSTANCE.getReaderForBasePath(baseDir));
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Could not instantiate base reader");
        }

        pathPartsStack.add(readerStack.peek().getWorkingDirectory());
        baseUri = "";
    }

    private IncrementalCompositingFSReader(@NotNull IncrementalCompositingFSReader other) {
        for (SimpleFSReader reader : other.readerStack)
            readerStack.push(reader);
        for (Path p : other.pathPartsStack)
            pathPartsStack.push(p);
        baseUri = other.baseUri;
    }

    @NotNull
    @Override
    public Collection<FileInfo> getDirectoryContents() throws IOException {
        return readerStack.peek().getDirectoryContents();
    }

    @NotNull
    @Override
    public FileInfo getFileByPath(@NotNull String pathName) throws IOException {
        Path relative = getReadOnlyRelativePath(pathName);
        if (relative != null) {
            return readerStack.peek().getFileByPath(relative);
        }

        FSReader<String> tmpReader = new IncrementalCompositingFSReader();
        tmpReader.changeDirectory(pathName);
        return tmpReader.getFileByPath(pathName);
    }

    @Override
    public synchronized boolean changeDirectory(@NotNull String pathName) throws IOException {
        IncrementalCompositingFSReader mock = new IncrementalCompositingFSReader(this);
        if (mock.updateStackForPath(pathName)) {
            this.readerStack = mock.readerStack;
            this.pathPartsStack = mock.pathPartsStack;
            this.baseUri = mock.baseUri;
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public String getWorkingDirectory() {
        StringBuilder sb = new StringBuilder(baseUri);

        for (Path p : pathPartsStack) {
            sb.append(p.toString());
        }

        return sb.toString();
    }

    @NotNull
    @Override
    public InputStream retrieveFileInputStream(@NotNull String pathName) throws IOException {
        Path relative = getReadOnlyRelativePath(pathName);
        if (relative != null) {
            return readerStack.peek().retrieveFileInputStream(relative);
        }

        FSReader<String> tmpReader = new IncrementalCompositingFSReader();
        tmpReader.changeDirectory(pathName);
        return tmpReader.retrieveFileInputStream(pathName);
    }

    private synchronized boolean updateStackForPath(@NotNull String pathName) throws IOException {

        if (PathHelper.isAbsolute(pathName)) {
            pathName = clearStackToBranchingPointAbsolute(pathName);
        } else {
            pathName = clearStackToBranchingPointRelative(pathName);
        }

        return buildStack(pathName);
    }

    @Nullable
    private Path getReadOnlyRelativePath(@NotNull String pathName) {
        final Path relative;
        if (PathHelper.isAbsolute(pathName)) {
            String base = PathHelper.getBase(pathName);
            if (!base.equals(baseUri))
                return null;

            String remaining = pathName.substring(base.length());

            remaining = checkBranchesInTopmostAndReturnTopmostPart(remaining);
            if (remaining == null)
                return null;

            relative = PathHelper.relativeFromAbsolute(pathPartsStack.peek(), remaining);
        } else {
            relative = PathHelper.toCorrespondingPath(pathPartsStack.peek(), pathName);

            if (PathHelper.resolveWithoutGoingDown(pathPartsStack.peek(), relative) == null)
                return null;
        }

        return relative;
    }

    @Nullable
    private String checkBranchesInTopmostAndReturnTopmostPart(@NotNull String remaining) {
        for (Path p : pathPartsStack) {
            // Excluding the topmost path
            if (p == pathPartsStack.peek())
                break;

            String lca = PathHelper.LCA(p, remaining);
            // Branches not in the outer reader
            if (!p.endsWith(lca)) {
                return null;
            }

            remaining = remaining.substring(lca.length());
        }
        return remaining;
    }

    private void popTopReader() {
        if (readerStack.isEmpty())
            return;
        readerStack.pop();
        pathPartsStack.pop();

        if (!readerStack.isEmpty()) {
            pathPartsStack.pop();
            pathPartsStack.push(readerStack.peek().getWorkingDirectory());
        }
    }

    private void trimStacksToSize(int size) {
        while (pathPartsStack.size() > size) {
            popTopReader();
        }
    }

    @NotNull
    private String clearStackToBranchingPointAbsolute(@NotNull String pathName) {
        String base = PathHelper.getBase(pathName);
        if (!base.equals(baseUri)) {
            readerStack.clear();
            pathPartsStack.clear();
            baseUri = "";
            return pathName;
        }

        String remaining = pathName.substring(base.length());
        int levelsProcessed = 0;
        for (Path p : pathPartsStack) {
            levelsProcessed++;
            // Excluding the topmost path
            if (p == pathPartsStack.peek())
                break;

            String lca = PathHelper.LCA(p, remaining);
            // Branches here
            if (!p.endsWith(lca)) {
                trimStacksToSize(levelsProcessed);
                break;
            }

            remaining = remaining.substring(lca.length());
        }

        return remaining;
    }

    @NotNull
    private String clearStackToBranchingPointRelative(@NotNull String pathName) {
        while (readerStack.size() > 0) {
            final Path root = pathPartsStack.peek().getRoot();
            final String delim = root.getFileSystem().getSeparator();
            final String[] parts = pathName.split(Pattern.quote(delim));

            Path curDir = readerStack.peek().getWorkingDirectory().normalize();
            int partsCount = countPartsToExitTopReader(root, parts, curDir);

            // Didn't clear top reader
            if (partsCount == -1)
                break;

            popTopReader();
            if (parts[partsCount].length() == 2)
                partsCount++;
            else
                parts[partsCount] = parts[partsCount].substring(2 + 1);

            pathName = PathHelper.joinCollection(Arrays.asList(parts).subList(partsCount, parts.length), delim);
        }

        return pathName;
    }

    private int countPartsToExitTopReader(@NotNull Path root, @NotNull String[] parts, @NotNull Path curDir) {
        boolean reachedRoot = false;

        int passed;
        for (passed = 0; passed < parts.length; ++passed) {
            if (!parts[passed].startsWith(".."))
                break;

            if (curDir.equals(root)) {
                reachedRoot = true;
                break;
            }

            curDir = curDir.resolve("..").normalize();
        }

        if (!reachedRoot)
            return -1;

        return passed;
    }

    private boolean buildStack(@NotNull String pathName) throws IOException {
        String remaining = pathName;
        if (readerStack.isEmpty()) {
            remaining = createBaseReaderAndReturnRemaining(remaining);
        }

        while (remaining.length() > 0) {
            remaining = createOneReaderAndReturnRemaining(remaining);

            if (remaining == null)
                return false;
        }

        return true;
    }

    @NotNull
    private String createBaseReaderAndReturnRemaining(@NotNull String pathName) throws IOException {
        String base = PathHelper.getBase(pathName);
        baseUri = base;
        pathName = pathName.substring(base.length());

        readerStack.push(SimpleFSReaderFactory.INSTANCE.getReaderForBasePath(base));
        pathPartsStack.push(readerStack.peek().getWorkingDirectory());

        return pathName;
    }

    @Nullable
    private String createOneReaderAndReturnRemaining(@NotNull String pathName) throws IOException {
        SimpleFSReader topReader = readerStack.peek();
        Path p = topReader.getWorkingDirectory().getFileSystem().getPath(pathName);

        if (topReader.changeDirectory(p)) {
            pathPartsStack.pop();
            pathPartsStack.push(topReader.getWorkingDirectory());
            return "";
        }

        // From here we understand that in the end we'll need more than one reader
        // To create it we need regular file to make reader from
        int foundFileIndex = findFirstRegularFileInPath(topReader, p);
        if (foundFileIndex == -1)
            return null;

        if (foundFileIndex > 1) {
            boolean chDirSuccess = topReader.changeDirectory(PathHelper.getPrefixPath(p, foundFileIndex - 1));
            assert chDirSuccess : "We could only switch to another reader here but could not chdir";
        }

        pathPartsStack.pop();
        pathPartsStack.push(topReader.getWorkingDirectory().resolve(p.getName(foundFileIndex - 1)));

        SimpleFSReader nextReader = createReaderForFile(pathPartsStack.peek());

        readerStack.push(nextReader);
        pathPartsStack.push(nextReader.getWorkingDirectory());

        if (foundFileIndex < p.getNameCount())
            return p.subpath(foundFileIndex, p.getNameCount()).toString();
        else
            return "";
    }

    private int findFirstRegularFileInPath(@NotNull SimpleFSReader topReader, @NotNull Path p) throws IOException {
        final int pathLen = p.getNameCount();

        int foundFileIndex = -1;
        for (int taken = 1; taken <= pathLen; ++taken) {
            Path prefixPath = PathHelper.getPrefixPath(p, taken);

            FileInfo f;
            try {
                f = topReader.getFileByPath(prefixPath);
            } catch (FileNotFoundException ignore) {
                return -1;
            }

            if (f.getAttributes().contains(FileInfo.FileAttribute.IS_REGULAR_FILE)) {
                foundFileIndex = taken;
                break;
            }
        }
        return foundFileIndex;
    }

    @NotNull
    private SimpleFSReader createReaderForFile(@NotNull Path path) throws IOException {
        String fileName = path.getFileName().toString();
        if (!SimpleFSReaderFactory.INSTANCE.isSupportedFileName(fileName))
            throw new UnsupportedDataTypeException("No FSReader for this file");

        Path newDir = Files.createTempDirectory("temp");
        // file's path may be from different provider
        Path tempFile = newDir.resolve(fileName);
        InputStream is = readerStack.peek().retrieveFileInputStream(path);

        Logger.getLogger("test").fine("Temp file " + tempFile);
        Files.copy(is, tempFile);
        return SimpleFSReaderFactory.INSTANCE.getReaderForFile(tempFile);
    }


}
