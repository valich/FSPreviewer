package org.valich.fsview.fsreader;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by valich on 21.03.14.
 */
public class PathHelper {
    private PathHelper() {}

    public static String getProtocol(String pathName) {
        int protocolPos = pathName.indexOf("://");
        if (protocolPos != -1 && protocolPos < pathName.indexOf("/")) {
            // Like `file:/'
            return pathName.substring(0, protocolPos + 2);
        }
        return null;
    }

    public static URI asURI(String pathName) {
        return asURI(pathName, null);
    }

    public static URI asURI(String pathName, String protocolIfNone) {
        if (protocolIfNone == null)
            protocolIfNone = "file:/";

        String protocol = getProtocol(pathName);

        if (protocol == null)
            pathName = protocolIfNone + "/" + pathName;

        try {
            return new URI(pathName);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isAbsolute(String pathName) {
        String protocol = getProtocol(pathName);

        if (protocol != null)
            return true;

        return Paths.get(pathName).isAbsolute();
    }

    public static String LCA(Path a, String b) {
        String delim = a.getFileSystem().getSeparator();
        String[] parts = b.split(delim);

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

    public static Path toCorrespondingPath(Path path, String pathName) {
        return path.getFileSystem().getPath(pathName).normalize();
    }

    public static Path relativeFromAbsolute(Path path, String absolute) {
        Path absolutePath = toCorrespondingPath(path, absolute);
        return path.relativize(absolutePath).normalize();
    }

    public static String getBase(String pathName) {
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

    public static Path resolveWithoutGoingDown(Path initial, Path relative) {
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

    public static Path getPrefixPath(Path p, int prefixLen) {
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
}
