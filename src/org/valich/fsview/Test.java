package org.valich.fsview;

import org.valich.fsview.fsreader.FSReader;
import org.valich.fsview.fsreader.IncrementalCompositingFSReader;

import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Scanner;

/**
 * Created by valich on 20.03.14.
 */
public class Test {
    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(new InputStreamReader(System.in));

        FSReader<String> fsReader = new IncrementalCompositingFSReader();

        outer: for (;;) {
            String cmd = in.next();
            switch (cmd) {
                case "exit":
                    break outer;
                case "cd":
                    boolean result = fsReader.changeDirectory(in.next());
                    System.out.println(result);
                    break;
                case "file_info":
                    FileInfo info = fsReader.getFileByPath(in.next());
                    System.out.println(info);
                    break;
                case "ls":
                    Collection<? extends FileInfo> dirs = fsReader.getDirectoryContents();
                    System.out.println(dirs);
                    break;
                case "pwd":
                    String curDir = fsReader.getWorkingDirectory();
                    System.out.println(curDir);
                    break;
            }
        }

        in.close();

    }
}
