package org.apache.tools.ant.taskdefs;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeleteTDTest {

    private DeleteTestable task;
    private File baseDir;
    private FileSystemD stubFs;
    private SpyObserver spyObserver;

    // Define a SpyObserver class to collect logs and errors for verification
    class SpyObserver implements TaskObserver {
        public List<String> logs = new ArrayList<>();
        public List<String> errors = new ArrayList<>();

        @Override
        public void logInfo(String message) { logs.add(message); }
        @Override
        public void handleErrorInfo(String errorMessage) { errors.add(errorMessage); }
    }

    @Before
    public void setUp() {
        task = new DeleteTestable();
        baseDir = new File("/fake/base");
        spyObserver = new SpyObserver();

        // Create a Stub File System
        stubFs = new FileSystemD() {
            @Override
            public File setFile(File parent, String childName) {
                return new File(parent, childName);
            }

            @Override
            public String[] list(File dir) {
                // Simulate a non-empty directory
                if (dir.getName().equals("notEmptyDir")) {
                    return new String[] {"hidden_file.txt"}; 
                }
                // Simulate null directories
                if (dir.getName().equals("nullDir")) {
                    return null; 
                }
                // Simulate empty directories
                return new String[0]; 
            }

            @Override
            public boolean delete(File f) {
                // Simulate an OS lock causing deletion failure
                if (f.getName().equals("locked.txt") || f.getName().equals("lockedEmptyDir")) {
                    return false; 
                }
                // Simulate successful deletion 
                return true; 
            }
        };
    }

    @Test
    public void testDeletesNormalFile() {
        // Act: Pass only normal.txt
        task.removeFilesTestable(baseDir, new String[]{"normal.txt"}, new String[]{}, true, stubFs, spyObserver);
        
        // Assert: Verify the successful deletion log
        assertTrue("Should successfully log the deletion of normal.txt.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("normal.txt")));
    }

    @Test
    public void testHandlesLockedFileError() {
        // Act: Pass only locked.txt
        task.removeFilesTestable(baseDir, new String[]{"locked.txt"}, new String[]{}, true, stubFs, spyObserver);
        
        // Assert: Verify the error handler was triggered
        assertTrue("Should handle error when locked.txt fails to delete.", 
            spyObserver.errors.stream().anyMatch(err -> err.contains("locked.txt")));
    }

    @Test
    public void testDeletesEmptyDir() {
        // Act: Pass only emptyDir
        task.removeFilesTestable(baseDir, new String[]{}, new String[]{"emptyDir"}, true, stubFs, spyObserver);
        
        // Assert
        assertTrue("Should successfully log the deletion of emptyDir.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("emptyDir")));
        assertTrue("Should log the summary for 1 directory.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("Deleted 1 directory")));
    }

    @Test
    public void testIgnoresNotEmptyDir() {
        // Act: Pass only notEmptyDir
        task.removeFilesTestable(baseDir, new String[]{}, new String[]{"notEmptyDir"}, true, stubFs, spyObserver);
        
        // Assert
        assertFalse("Should NOT attempt to delete or log notEmptyDir.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("notEmptyDir")));
    }

    @Test
    public void testHandlesLockedDirectoryError() {
        // Act: Pass only a single empty directory lockedEmptyDir that CANNOT be deleted
        task.removeFilesTestable(baseDir, new String[]{}, new String[]{"lockedEmptyDir"}, true, stubFs, spyObserver);
        
        // Assert:
        // 1. Verify it enters the first 'if' block and logs
        assertTrue("Should log the attempt to delete lockedEmptyDir.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("lockedEmptyDir")));
            
        // 2. Verify it enters the second 'if' block and correctly calls the error handler
        assertTrue("Should handle error when lockedEmptyDir fails to delete.", 
            spyObserver.errors.stream().anyMatch(err -> err.contains("Unable to delete directory") && err.contains("lockedEmptyDir")));
    }

    @Test
    public void testHandlesNullDirectory() {
        // Act: Pass a directory "nullDir" return null
        task.removeFilesTestable(baseDir, new String[]{}, new String[]{"nullDir"}, true, stubFs, spyObserver);
        
        // Assert: 
        // Verify no NullPointerException occurs and it correctly treats it as an empty directory
        assertTrue("Should successfully log the deletion of nullDir.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("nullDir")));
        assertTrue("Should log the summary for 1 directory.", 
            spyObserver.logs.stream().anyMatch(msg -> msg.contains("Deleted 1 directory")));
    }
}
