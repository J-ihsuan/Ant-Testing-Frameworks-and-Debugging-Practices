package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CarolCopyTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        // Point to copy_test.xml
        buildRule.configureProject("src/etc/testcases/taskdefs/copy_test.xml");
    }

    /**
     * Partition 1: Valid Source
     * Test: Duplicate an existed file
     * Expection: dest.txt should be created
     */
    @Test
    public void testValidCopy() {
        buildRule.executeTarget("setUp");
        buildRule.executeTarget("testValidCopy");

        File destFile = new File("src/etc/testcases/taskdefs/copy-temp/dest.txt");
        assertTrue("Destination file should exist after copy", destFile.exists());

        buildRule.executeTarget("tearDown");
    }

    /**
     * Partition 2: Missing Source
     * Testing: Duplicate an inexisted file (ghost.txt)
     * Expectation: BuildException
     */
    @Test
    public void testMissingSource() {
        buildRule.executeTarget("setUp");

        try {
            buildRule.executeTarget("testMissingSource");
            // If the program doesn't show this error code -> fail
            fail("Build should have failed due to missing source file");
        } catch (BuildException ex) {
            // Catch the bug successfully
        }

        buildRule.executeTarget("tearDown");
    }

    /**
     * Partition 3: Directory Copy
     * Testing: Duplicate a file
     * Expectation: Object file should appear
     */
    @Test
    public void testDirectoryCopy() {
        buildRule.executeTarget("setUp");
        buildRule.executeTarget("testDirectoryCopy");

        File destDir = new File("src/etc/testcases/taskdefs/copy-temp/dest_dir");
        assertTrue("Destination directory should exist", destDir.exists());

        buildRule.executeTarget("tearDown");
    }
}