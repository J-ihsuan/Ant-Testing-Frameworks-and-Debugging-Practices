package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class CarolCopyFsmTest {
    private Project project;
    private Copy copyTask;
    private File tempDir;

    @Before
    public void setUp() {
        project = new Project();
        project.init();
        copyTask = new Copy();
        copyTask.setProject(project);
        
        // Establish a temporary testing table of contents
        tempDir = new File("fsm_test_tmp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    /**
     * Test Path: IDLE -> VALIDATING -> SCANNING -> PREPARING -> COPYING -> FINISHED
     * Verifies a complete successful copy operation.
     */
    @Test
    public void testFullSuccessPath() throws Exception {
        File src = new File(tempDir, "source.txt");
        File dest = new File(tempDir, "dest.txt");
        Files.write(src.toPath(), "Hello Carol's FSM Test".getBytes());

        copyTask.setFile(src);
        copyTask.setTofile(dest);
        copyTask.execute();

        assertTrue("File should be successfully copied to destination", dest.exists());
        assertEquals("File content should match", "Hello Carol's FSM Test", new String(Files.readAllBytes(dest.toPath())));
    }

    /**
     * Test Path: VALIDATING -> FAILED
     * Verifies that missing mandatory attributes (like todir/tofile) triggers a BuildException.
     */
    @Test(expected = BuildException.class)
    public void testMissingDestDir() {
        File src = new File(tempDir, "source.txt");
        copyTask.setFile(src);
        // Intentional omission of setTofile() or setTodir()
        copyTask.execute();
    }

    /**
     * Test Path: SCANNING -> FAILED
     * Verifies that a non-existent source file leads to a failure state.
     */
    @Test(expected = BuildException.class)
    public void testSourceFileMissing() {
        File ghostFile = new File(tempDir, "ghost.txt");
        if (ghostFile.exists()) ghostFile.delete();

        copyTask.setFile(ghostFile);
        copyTask.setTodir(tempDir);
        copyTask.execute();
    }
}