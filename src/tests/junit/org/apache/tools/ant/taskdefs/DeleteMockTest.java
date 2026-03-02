package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.mockito.Mockito.*;

public class DeleteMockTest {

    private Delete deleteTask;
    private Project mockProject;
    private File mockFile;

    @Before
    public void setUp() {
        // Initialize the task and setup mock objects
        deleteTask = new Delete();
        mockProject = mock(Project.class);
        mockFile = mock(File.class);
        
        // Inject the mocked Project to intercept and verify logging behavior
        deleteTask.setProject(mockProject);
        
        // Stub the absolute path to ensure predictable log output verification
        when(mockFile.getAbsolutePath()).thenReturn("/fake/base/dummy_dir");
        
        // Provide a real Path object instead of a mock to prevent NullPointerExceptions
        // when invoking java.nio.file.Files.isSymbolicLink(f.toPath())
        when(mockFile.toPath()).thenReturn(Paths.get("/fake/base/dummy_dir"));
        
        // Inject the mocked File object into the Delete task
        deleteTask.setFile(mockFile);
    }


    // --- Test Case 1: When file is a directory ---
    @Test
    public void testFileIsDir() {
        // Simulate a file exists but is a directory
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.isDirectory()).thenReturn(true);

        // Execute
        deleteTask.execute();

        // Assert: Verify the warning was logged with the MSG_VERBOSE
        // Note: Ant's Task.log() internally delegates to Project.log(Task, String, int)
        verify(mockProject, times(1)).log(
            eq(deleteTask),
            eq("Directory /fake/base/dummy_dir cannot be removed using the file attribute.  Use dir instead."), 
            eq(Project.MSG_VERBOSE)
        );
        // Behavior Checking: Ensure delete() operation was skipped
        verify(mockFile, never()).delete();
    }


    // --- Test Case 2: when file does not exist ---
    @Test
    public void testFileNotExist() {
        // Simulate a non-existent file
        when(mockFile.exists()).thenReturn(false);

        // Execute
        deleteTask.execute();

        // Assert: Verify the task handled correctly and logged it
        verify(mockProject, times(1)).log(
            eq(deleteTask),
            eq("Could not find file /fake/base/dummy_dir to delete."), 
            eq(Project.MSG_VERBOSE)
        );

        // Behavior Checking: Ensure delete() operation was skipped
        verify(mockFile, never()).delete();
    }


    // --- Test Case 3: When a normal file is deleted ---
    @Test
    public void testFileIsNormal() {
        // Simulate a standard, existing file
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.isDirectory()).thenReturn(false);
        
        // Stub the deletion process to simulate delete successfully
        when(mockFile.delete()).thenReturn(true);

        // Execute
        deleteTask.execute();

        // Assert: Verify the task was logged with the MSG_INFO
        verify(mockProject, times(1)).log(
            eq(deleteTask),
            eq("Deleting: /fake/base/dummy_dir"),
            eq(Project.MSG_INFO)
        );

        // Behavior Checking: Ensure delete() operation was executed
        verify(mockFile, atLeastOnce()).delete();
    }

    
    // --- Test Case 4: When file deletion fails ---
    
    // This test should only pass if a BuildException is thrown
    @Test(expected = org.apache.tools.ant.BuildException.class)
    public void testFileCantDelete() {
        // Simulate a standard, existing file
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.isDirectory()).thenReturn(false);
        
        // Stub: Simulate the file is locked causing delete() method return false.
        when(mockFile.delete()).thenReturn(false);

        // Execute
        deleteTask.execute();

        // Assert: 
        // handle() method in Delete.java will be triggered, which throws a BuildException
        // No Mockito verify() is needed here
    }
}