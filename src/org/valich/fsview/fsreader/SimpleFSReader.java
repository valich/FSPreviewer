package org.valich.fsview.fsreader;

import java.nio.file.Path;

/**
 * Convenience hierarchy subinterface representing FSReader that is able
 * to work with one particular Path type.
 */
interface SimpleFSReader extends FSReader<Path> {
}
