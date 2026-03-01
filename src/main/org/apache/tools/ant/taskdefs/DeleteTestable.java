package org.apache.tools.ant.taskdefs;
import java.io.File;

// Interfaces for Dependency Injection
interface FileSystemD {
    File setFile(File parent, String childName);
    String[] list(File dir);
    boolean delete(File f);
}

interface TaskObserver {
    void logInfo(String message);
    void handleErrorInfo(String errorMessage);
}

// Refactored Class and Method
public class DeleteTestable extends Delete {

    // Overloaded dummy method utilizing the new testable design
    protected void removeFilesTestable(File d, String[] files, String[] dirs, boolean includeEmpty, 
                                       FileSystemD fs, TaskObserver observer) {
        if (files.length > 0) {
            observer.logInfo("Deleting " + files.length + " files from " + d.getAbsolutePath());
            for (String filename : files) {
                File f = fs.setFile(d, filename);
                observer.logInfo("Deleting " + f.getAbsolutePath());
                if (!fs.delete(f)) {
                    observer.handleErrorInfo("Unable to delete file " + f.getAbsolutePath());
                }
            }
        }
        if (dirs.length > 0 && includeEmpty) {
            int dirCount = 0;
            for (int j = dirs.length - 1; j >= 0; j--) {
                File currDir = fs.setFile(d, dirs[j]);
                String[] dirFiles = fs.list(currDir); 
                
                // Only delete if dir empty
                if (dirFiles == null || dirFiles.length == 0) {
                    observer.logInfo("Deleting " + currDir.getAbsolutePath());
                    if (!fs.delete(currDir)) {
                        observer.handleErrorInfo("Unable to delete directory " + currDir.getAbsolutePath());
                    } else {
                        dirCount++;
                    }
                }
            }

            if (dirCount > 0) {
                observer.logInfo("Deleted " + dirCount
                     + " director" + (dirCount == 1 ? "y" : "ies")
                     + " from " + d.getAbsolutePath());
            }
        }
    }
}
