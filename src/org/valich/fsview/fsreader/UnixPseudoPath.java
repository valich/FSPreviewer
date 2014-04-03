package org.valich.fsview.fsreader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

class UnixPseudoPath implements Path {
    private final boolean isAbsolute;
    private final List<String> parts = new ArrayList<>();

    UnixPseudoPath() {
        isAbsolute = true;
        parts.add("/");
    }

    private UnixPseudoPath(String... path) {
        this(path[0].charAt(0) == '/', Arrays.asList(path));
    }

    private UnixPseudoPath(boolean isAbsolute, List<String> path) {
        String bigStr = PathHelper.joinCollection(path, "/");
        this.isAbsolute = isAbsolute;

        String[] preparts = bigStr.split("/");
        for (String s : preparts) {
            if (s != null && s.length() > 0)
                parts.add(s);
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return new UnixPseudoFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return isAbsolute;
    }

    @Override
    public Path getRoot() {
        return getFileSystem().getRootDirectories().iterator().next();
    }

    @Override
    public Path getFileName() {
        if (parts.size() == 0)
            return null;
        return new UnixPseudoPath(parts.get(parts.size() - 1));
    }

    @Override
    public Path getParent() {
        if (parts.size() == 0)
            return null;
        if (parts.size() == 1)
            return getRoot();

        return new UnixPseudoPath(isAbsolute(), parts.subList(0, parts.size() - 1));
    }

    @Override
    public int getNameCount() {
        return parts.size();
    }

    @Override
    public Path getName(int index) {
        return new UnixPseudoPath(parts.get(index));
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return new UnixPseudoPath(isAbsolute() && (beginIndex == 0),
                parts.subList(beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(other instanceof UnixPseudoPath))
            throw new IllegalArgumentException();

        UnixPseudoPath o = (UnixPseudoPath) other;
        int minL = Math.min(parts.size(), o.parts.size());
        return parts.subList(0, minL).equals(o.parts.subList(0, minL));
    }

    @Override
    public boolean startsWith(String other) {
        return startsWith(new UnixPseudoPath(other));
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(other instanceof UnixPseudoPath))
            throw new IllegalArgumentException();

        UnixPseudoPath o = (UnixPseudoPath) other;
        int minL = Math.min(parts.size(), o.parts.size());
        return parts.subList(parts.size() - minL, parts.size())
                .equals(o.parts.subList(o.parts.size() - minL, o.parts.size()));
    }

    @Override
    public boolean endsWith(String other) {
        return endsWith(new UnixPseudoPath(other));
    }

    @Override
    public Path normalize() {
        // quadratic
        LinkedList<String> tmpList = new LinkedList<>(parts);

        boolean changed = true;
        while (tmpList.size() > 0 && changed) {
            changed = false;

            String prev = "";
            for (ListIterator<String> it = tmpList.listIterator(); it.hasNext(); ) {
                int index = it.nextIndex();
                String s = it.next();
                if (s.equals(".")) {
                    changed = true;
                    it.remove();
                    break;
                } else if (index > 0 && s.equals("..") && !prev.equals("..")) {
                    changed = true;
                    it.remove();
                    it.previous();
                    it.remove();
                    break;
                }

                prev = s;
            }
        }

        if (tmpList.size() == 0)
            return getRoot();

        return new UnixPseudoPath(isAbsolute(), tmpList);
    }

    @Override
    public Path resolve(Path other) {
        if (!(other instanceof UnixPseudoPath))
            throw new IllegalArgumentException();

        if (other.isAbsolute())
            return other;

        List<String> all = new ArrayList<>(parts);
        all.addAll(((UnixPseudoPath) other).parts);

        return new UnixPseudoPath(isAbsolute(), all);
    }

    @Override
    public Path resolve(String other) {
        return resolve(new UnixPseudoPath(other));
    }

    @Override
    public Path resolveSibling(Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolveSibling(String other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path relativize(Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path toAbsolutePath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private final Iterator<String> inner = parts.iterator();

            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public Path next() {
                return new UnixPseudoPath(inner.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int compareTo(Path other) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof UnixPseudoPath))
            return false;

        UnixPseudoPath p = ((UnixPseudoPath) o);
        return isAbsolute == p.isAbsolute && parts.equals(p.parts);
    }

    @Override
    public String toString() {
        String res = PathHelper.joinCollection(parts, "/");
        if (isAbsolute())
            return "/" + res;
        return res;
    }

    private class UnixPseudoFileSystem extends FileSystem {

        @Override
        public FileSystemProvider provider() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public String getSeparator() {
            return "/";
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            Path root = getPath("/");
            return Collections.singletonList(root);
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path getPath(String first, String... more) {
            List<String> all = new ArrayList<>();
            all.add(first);
            all.addAll(Arrays.asList(more));
            return new UnixPseudoPath(first.charAt(0) == '/', all);
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WatchService newWatchService() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
