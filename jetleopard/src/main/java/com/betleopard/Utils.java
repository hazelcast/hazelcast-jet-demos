package com.betleopard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Holder for associated helper methods
 * 
 * @author kittylyst
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Moves a file from a resource path to a temp dir under /tmp. 
     * 
     * @param resourceName a {@code String} representing the resource to be moved
     * @return             a {@code Path} to the moved file
     * @throws IOException a general, unexpected IO failure
     */
    public static Path unpackDataToTmp(final String resourceName) throws IOException {
        final InputStream in = Utils.class.getResourceAsStream("/" + resourceName);
        final Path tmpdir = Files.createTempDirectory(Paths.get("/tmp"), "hc-spark-test");
        final Path dataFile = tmpdir.resolve(resourceName);
        Files.copy(in, dataFile, StandardCopyOption.REPLACE_EXISTING);

        return dataFile;
    }

    /**
     * Cleans up the temp directory.
     * 
     * @param tmpdir       the directory to be cleaned (the parent of the file that was moved)
     * @throws IOException a general, unexpected IO failure
     */
    public static void cleanupDataInTmp(final Path tmpdir) throws IOException {
        Files.walkFileTree(tmpdir, new Reaper());
    }

    /**
     * A helper class that can be used to recursively delete and clean up
     * the temp dir
     */
    public static final class Reaper extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc == null) {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            } else {
                throw exc;
            }
        }
    }

}
