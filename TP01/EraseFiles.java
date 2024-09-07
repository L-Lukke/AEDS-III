import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class EraseFiles {

    // Method to delete all files in a directory without deleting the directory itself
    public static boolean eraseAllFilesInDirectory(String directoryPath) {
        Path dir = Paths.get(directoryPath);
        
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // Delete each file
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}