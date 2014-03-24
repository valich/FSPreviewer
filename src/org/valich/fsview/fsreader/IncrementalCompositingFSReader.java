package org.valich.fsview.fsreader;

import org.valich.fsview.FileInfo;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * Created by valich on 21.03.14.
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

    private IncrementalCompositingFSReader(IncrementalCompositingFSReader other) {
        for (SimpleFSReader reader : other.readerStack)
            readerStack.push(reader);
        for (Path p : other.pathPartsStack)
            pathPartsStack.push(p);
        baseUri = other.baseUri;
    }

    @Override
    public Collection<? extends FileInfo> getDirectoryContents() {
        return readerStack.peek().getDirectoryContents();
    }

    @Override
    public FileInfo getFileByPath(String pathName) {
        Path relative = getReadOnlyRelativePath(pathName);
        if (relative != null) {
            return readerStack.peek().getFileByPath(relative);
        }

        FSReader<String> tmpReader = new IncrementalCompositingFSReader();
        tmpReader.changeDirectory(pathName);
        return tmpReader.getFileByPath(pathName);
    }

    @Override
    public synchronized boolean changeDirectory(String pathName) {
        IncrementalCompositingFSReader mock = new IncrementalCompositingFSReader(this);
        if (mock.updateStackForPath(pathName)) {
            this.readerStack = mock.readerStack;
            this.pathPartsStack = mock.pathPartsStack;
            this.baseUri = mock.baseUri;
            return true;
        }
        return false;
    }

    @Override
    public String getWorkingDirectory() {
        StringBuilder sb = new StringBuilder(baseUri);

//        for (FSReader reader : readerStack) {
//            sb.append(reader.getWorkingDirectory());
//        }
        for (Path p : pathPartsStack) {
            sb.append(p.toString());
        }

        return sb.toString();
    }

    @Override
    public InputStream retrieveFileInputStream(String pathName) throws IOException {
        Path relative = getReadOnlyRelativePath(pathName);
        if (relative != null) {
            return readerStack.peek().retrieveFileInputStream(relative);
        }

        FSReader<String> tmpReader = new IncrementalCompositingFSReader();
        tmpReader.changeDirectory(pathName);
        return tmpReader.retrieveFileInputStream(pathName);
    }

    private Path getReadOnlyRelativePath(String pathName) {
        final Path relative;
        if (PathHelper.isAbsolute(pathName)) {
            String base = PathHelper.getBase(pathName);
            if (!base.equals(baseUri))
                return null;

            String remaining = pathName.substring(base.length());
            for (Path p : pathPartsStack) {
                // Excluding the topmost path
                if (p == pathPartsStack.peek())
                    break;

                String lca = PathHelper.LCA(p, remaining);
                // Branches not in the outer reader
                if (!p.endsWith(lca))
                    return null;

                remaining = remaining.substring(lca.length());
            }

            relative = PathHelper.relativeFromAbsolute(pathPartsStack.peek(), remaining);
        } else {
            relative = PathHelper.toCorrespondingPath(pathPartsStack.peek(), pathName);

            if (PathHelper.resolveWithoutGoingDown(pathPartsStack.peek(), relative) == null)
                return null;
        }

        return relative;
    }

    private synchronized boolean updateStackForPath(String pathName) {

        if (PathHelper.isAbsolute(pathName)) {
            pathName = clearStackToBranchingPointAbsolute(pathName);
        } else {
            pathName = clearStackToBranchingPointRelative(pathName);
        }

        return buildStack(pathName);
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

    private String clearStackToBranchingPointAbsolute(String pathName) {
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

    private void trimStacksToSize(int size) {
        while (pathPartsStack.size() > size) {
            popTopReader();
        }
    }

    private String clearStackToBranchingPointRelative(String pathName) {
        while (readerStack.size() > 0) {
            final Path root = pathPartsStack.peek().getRoot();
            final String delim = root.getFileSystem().getSeparator();
            final String[] parts = pathName.split(delim);

            Path resolved = readerStack.peek().getWorkingDirectory().normalize();
            boolean clearedTopReader = false;
            int passed;
            for (passed = 0; passed < parts.length; ++passed) {
                if (!parts[passed].startsWith(".."))
                    break;
                if (resolved.equals(root)) {
                    clearedTopReader = true;
                    break;
                }

                resolved = resolved.resolve("..").normalize();
            }

            if (!clearedTopReader)
                break;

            popTopReader();
            if (parts[passed].length() == 2)
                passed++;
            else
                parts[passed] = parts[passed].substring(2 + 1);

            pathName = joinCollection(Arrays.asList(parts).subList(passed, parts.length), delim);
        }

        return pathName;
    }

    private static String joinCollection(Collection<String> arr, String delim) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String s : arr) {
            if (!isFirst)
                sb.append(delim);
            isFirst = false;
            sb.append(s);
        }

        return sb.toString();
    }

    private boolean buildStack(String pathName) {
        if (/*PathHelper.isAbsolute(pathName) || */readerStack.isEmpty()) {
//            assert readerStack.isEmpty() : "Initializing non-empty stack";

            String base = PathHelper.getBase(pathName);
            baseUri = base;
            pathName = pathName.substring(base.length());

            try {
                readerStack.push(SimpleFSReaderFactory.INSTANCE.getReaderForBasePath(base));
                pathPartsStack.push(readerStack.peek().getWorkingDirectory());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        while (pathName.length() > 0) {
            SimpleFSReader topReader = readerStack.peek();
            Path p = topReader.getWorkingDirectory().getFileSystem().getPath(pathName);

            if (topReader.changeDirectory(p)) {
                pathPartsStack.pop();
                pathPartsStack.push(topReader.getWorkingDirectory());
                return true;
            }

            int foundFileIndex = -1;
            final int pathLen = p.getNameCount();
            for (int taken = 1; taken <= pathLen; ++taken) {
                Path prefixPath = PathHelper.getPrefixPath(p, taken);

                FileInfo f = topReader.getFileByPath(prefixPath);
                if (f != null && f.getAttributes().contains(FileInfo.FileAttribute.IS_REGULAR_FILE)) {
                    foundFileIndex = taken;
                    break;
                }
            }

            if (foundFileIndex == -1)
                return false;

            boolean changed = true;
            if (foundFileIndex > 1)
                changed = topReader.changeDirectory(PathHelper.getPrefixPath(p, foundFileIndex - 1));
            assert changed;

            pathPartsStack.pop();
            pathPartsStack.push(topReader.getWorkingDirectory().resolve(p.getName(foundFileIndex - 1)));

            SimpleFSReader nextReader = createReaderForFile(pathPartsStack.peek());
            if (nextReader == null)
                return false;

            readerStack.push(nextReader);
            pathPartsStack.push(nextReader.getWorkingDirectory());
            if (foundFileIndex < pathLen)
                pathName = p.subpath(foundFileIndex, pathLen).toString();
            else
                pathName = "";
        }

        return true;
    }

    private SimpleFSReader createReaderForFile(Path path) {
        try {
            String fileName = path.getFileName().toString();
            if (!SimpleFSReaderFactory.INSTANCE.isSupportedFileName(fileName)) {
                return null;
            }

            Path newDir = Files.createTempDirectory("temp");
            // file's path may be from different provider
            Path tempFile = newDir.resolve(fileName);
            InputStream is = readerStack.peek().retrieveFileInputStream(path);
            if (is == null)
                return null;

            Logger.getLogger("test").fine("Temp file " + tempFile);
            Files.copy(is, tempFile);
            return SimpleFSReaderFactory.INSTANCE.getReaderForFile(tempFile);
        } catch (IOException e) {
            Logger.getLogger("test").warning("Error creating FSReader: " + e.toString());
            return null;
        }
    }


}
