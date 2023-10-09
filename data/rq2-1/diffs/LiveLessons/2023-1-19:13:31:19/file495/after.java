package utils;

import java.io.File;

/**
 * A Java utility class that provides helper methods for file
 * operations.
 */
public final class FileUtils {
    /**
     * Logging tag.
     */
    private static final String TAG = FileUtils.class.getName();

    /**
     * A utility class should always define a private constructor.
     */
    private FileUtils() {
    }

    /**
     * Clears the filter directories.
     */
    public static void deleteDownloadedImages() {
        int deletedFiles =
            deleteSubFolders(Options.instance().getDirectoryPath());

        if (Options.instance().diagnosticsEnabled())
            System.out.println(TAG
                               + ": "
                               + deletedFiles
                               + " previously downloaded file(s) deleted");
    }

    /**
     * Recursively delete files in a specified folder.
     */
    public static int deleteSubFolders(String path) {
        int deletedFiles = 0;
        File currentFolder = new File(path);
        File[] files = currentFolder.listFiles();

        if (files == null) 
            return 0;

        // Java doesn't delete a directory with child files, so we
        // need to write code that handles this recursively.
        for (File f : files) {          
            if (f.isDirectory()) 
                deletedFiles += deleteSubFolders(f.toString());
            f.delete();
            deletedFiles++;
        }

        // Don't delete the current folder.
        // currentFolder.delete();
        return deletedFiles;
    }
}
