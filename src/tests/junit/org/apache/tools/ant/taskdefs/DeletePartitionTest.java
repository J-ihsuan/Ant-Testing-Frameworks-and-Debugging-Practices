package org.apache.tools.ant.taskdefs;


import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DeletePartitionTest {

    /**
     * Instantiate and configure a new Ant Project to ensures each test 
     * runs in an isolated environment with a correct base directory.
     */
    private Project createProject() {
        Project project = new Project();
        project.setBaseDir(new File(System.getProperty("user.dir")));
        return project;
    }

    // --- Partition 1: Existing File ---
    @Test
    public void testExistFile() throws IOException {
        // Initialize project and create a standard file
        Project project = createProject();
        File p1 = new File("testDelete.txt");
        if (!p1.exists()) {
            p1.createNewFile();
        }
        
        // Execute
        Delete d = new Delete();
        d.setProject(project);  // Inject the project environment
        d.setFile(p1);          // Set the target attribute
        d.execute();
        
        // Assert
        assertFalse("Delete Existing File Failed: File should be deleted", p1.exists());
    }

    // --- Partition 2: Missing File ---
    @Test
    public void testMissFile() {
        Project project = createProject();
        File p2 = new File("testDelete.txt");
        if (p2.exists()) {
            p2.delete();
        }

        Delete d = new Delete();
        d.setProject(project);
        d.setFile(p2);
        
        try {
            d.execute(); 
        } catch (BuildException e) {
            // Expected
        }

        assertFalse("Delete Missing File Failed: File should still not exist", p2.exists());
    }

    // --- Partition 3: Empty Directory ---
    @Test
    public void testEmptyDir() {
        Project project = createProject();
        File p3 = new File("empty_dir");
        p3.mkdir();

        Delete d = new Delete();
        d.setProject(project);
        d.setDir(p3);
        d.execute();

        assertFalse("Delete Empty Directory Failed: Empty directory should be deleted", p3.exists());
    }

    // --- Partition 4: Non-empty Directory ---
    @Test
    public void testNonEmptyDir() throws IOException {
        Project project = createProject();
        File p4 = new File("nonempty_dir");
        p4.mkdir();
        File child = new File(p4, "child.txt");
        child.createNewFile();

        Delete d = new Delete();
        d.setProject(project);
        d.setDir(p4);
        d.execute();

        assertFalse("Delete Non-empty Directory Failed: Child file should be deleted", child.exists());
        assertFalse("Delete Non-empty Directory Failed: Directory should be deleted", p4.exists());
    }    
}

