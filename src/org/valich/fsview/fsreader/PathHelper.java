package org.valich.fsview.fsreader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Pattern;

final class PathHelper {
    private PathHelper() {}

    @Nullable
    public static String getProtocol(@NotNull String pathName) {
        int protocolPos = pathName.indexOf("://");
        if (protocolPos != -1 && protocolPos < pathName.indexOf("/")) {
            // Like `file:/'
            return pathName.substring(0, protocolPos + 2);
        }
        return null;
    }

    public static boolean isAbsolute(@NotNull String pathName) {
        String protocol = getProtocol(pathName);

        if (protocol != null)
            return true;

        return Paths.get(pathName).isAbsolute();
    }

    @NotNull
    public static String LCA(@NotNull Path a, @NotNull String b) {
        String delim = a.getFileSystem().getSeparator();
        String[] parts = b.split(Pattern.quote(delim));

        Path p = a.getRoot();
        StringBuilder answerB = new StringBuilder();
        boolean isFirst = true;
        for (String part : parts) {
            p = p.resolve(part).normalize();
            if (!a.startsWith(p))
                break;

            if (!isFirst)
                answerB.append(delim);
            isFirst = false;
            answerB.append(part);
        }

        String answer = answerB.toString();
        assert b.startsWith(answer);

        return answer;
    }

    @NotNull
    public static Path toCorrespondingPath(@NotNull Path path, @NotNull String pathName) {
        return path.getFileSystem().getPath(pathName).normalize();
    }

    @NotNull
    public static Path relativeFromAbsolute(@NotNull Path path, @NotNull String absolute) {
        Path absolutePath = toCorrespondingPath(path, absolute);
        return path.relativize(absolutePath).normalize();
    }

    @NotNull
    public static String getBase(@NotNull String pathName) {
        String protocol = getProtocol(pathName);
        if (protocol == null) {
            return "";
        }

        URI uri;
        try {
            uri = new URI(pathName);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    @Nullable
    public static Path resolveWithoutGoingDown(@NotNull Path initial, @NotNull Path relative) {
        if (relative.isAbsolute())
            return relative;

        final Path root = initial.getRoot();

        Path resolved = initial.normalize();
        for (Path p : relative) {
            if (p.startsWith("..") && resolved.equals(root))
                return null;

            resolved = resolved.resolve(p).normalize();
        }

        return resolved;
    }

    @NotNull
    public static Path getPrefixPath(@NotNull Path p, int prefixLen) {
        if (prefixLen == 0)
            if (p.isAbsolute())
                return p.getRoot();
            else
                return p.getFileSystem().getPath("");

        if (prefixLen > p.getNameCount())
            prefixLen = p.getNameCount();

        Path result = p.subpath(0, prefixLen);
        if (p.isAbsolute())
            return p.getRoot().resolve(result);
        else
            return result;
    }

    @NotNull
    public static String joinCollection(@NotNull Collection<String> arr, @NotNull String delim) {
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
}
