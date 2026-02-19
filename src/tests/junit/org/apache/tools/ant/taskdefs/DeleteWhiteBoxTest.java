package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.*;
import org.apache.tools.ant.types.Mapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeleteWhiteBoxTest {

    /*
     * Ｃreate a fresh temporary directory for every test case to
     * ensure no side effects between tests and handles cleanup automatically.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Project project;
    private Delete deleteTask;

    @Before
    public void setUp() {
        /* Instantiate and configure a new Ant Project to ensures each test 
         * runs in an isolated environment
         */ 
        project = new Project();
        project.init();
        project.setBaseDir(tempFolder.getRoot());
        
        deleteTask = new Delete();
        deleteTask.setProject(project);
    }

    /*
     * Test Scenario 1: Direct Invocation of removeFiles Method
     *
     * Purpose: tests the deletion method: removeFiles(File, String[], String[]).
     */
    @Test
    public void testRemoveFilesDirectly() throws IOException {
        // Base directory for scanning
        File dir = tempFolder.newFolder("scanner");
        deleteTask.setDir(dir);
        deleteTask.setIncludeEmptyDirs(true); 

        // Create files and directories 
        File fileToDelete1 = new File(dir, "delete1.txt");
        File fileToDelete2 = new File(dir, "delete2.txt");
        File fileToKeep = new File(dir, "keep.txt");
        File dirToDelete1 = new File(dir, "deleteDir1");
        File dirToDelete2 = new File(dir, "deleteDir2");
        
        fileToDelete1.createNewFile();
        fileToDelete2.createNewFile();
        fileToKeep.createNewFile();
        dirToDelete1.mkdir();
        dirToDelete2.mkdir();

        // Simulate the arrays returned by Ant's DirectoryScanner after filtering
        String[] files = new String[] { "delete1.txt", "delete2.txt" };
        String[] dirs = new String[] { "deleteDir1", "deleteDir2" };

        // Execute
        deleteTask.removeFiles(dir, files, dirs);

        // Verify the results
        assertFalse("File specified in the files array should be deleted", fileToDelete1.exists());
        assertFalse("File specified in the files array should be deleted", fileToDelete2.exists());
        assertFalse("Directory specified in the dirs array should be deleted", dirToDelete1.exists());
        assertFalse("Directory specified in the dirs array should be deleted", dirToDelete2.exists());
        assertTrue("File not specified in the array should be preserved", fileToKeep.exists());
    }

   /*
     * Test Scenario 2: Content and Filename Based Deletion (Multi Add Selectors)
     * 
     * Purpose: Validates that when multiple selectors are added directly to the task, 
     * they act as a logical AND (Intersection).
     * 
     * Rule: Delete files that BOTH end with ".txt" AND contain the keyword "ERROR"
     */
    @Test
    public void testDeleteByContentAndFilename() throws IOException {
        File dir = tempFolder.newFolder("logs");
        deleteTask.setDir(dir);

        // File 1: Correct extension, wrong content
        File keep1 = new File(dir, "02012026.txt");
        Files.write(keep1.toPath(), "All systems normal".getBytes()); 

        // File 2: Wrong extension, correct content
        File keep2 = new File(dir, "02012026.log");
        Files.write(keep2.toPath(), "ERROR: testing system crash".getBytes());      

        // File 3: Correct extension AND content (Match)
        File nokeep = new File(dir, "02022026.txt");
        Files.write(nokeep.toPath(), "Network ERROR occurred".getBytes()); // Match both
        
        // File 4: Wrong both
        File keep3 = new File(dir, "02032026.log");
        Files.write(keep3.toPath(), "no record".getBytes()); 

        // Selector 1: File name must end with ".txt"
        FilenameSelector f1Selector = new FilenameSelector();
        f1Selector.setName("*.txt");
        deleteTask.addFilename(f1Selector);

        // Selector 2: File content must contain "ERROR"
        ContainsSelector cSelector = new ContainsSelector();
        cSelector.setText("ERROR");
        deleteTask.addContains(cSelector);

        deleteTask.execute();

        assertTrue("File with correct name but no keyword should be kept", keep1.exists());
        assertTrue("File with correct keyword but wrong name should be kept", keep2.exists());
        assertTrue("File with wrong keyword and name should be kept", keep3.exists());
        assertFalse("File with correct keyword and name should be deleted", nokeep.exists());
    }

    /*
     * Test Scenario 3: PatternSet
     * 
     * Purpose: tests the creation of include/exclude patterns to 
     * clean up *.class files while preserving test classes.
     */
    @Test
    public void testDeleteUsingPatterns() throws IOException {
        File dir = tempFolder.newFolder("build");
        deleteTask.setDir(dir);

        File mainClass = new File(dir, "Main.class");
        File testClass = new File(dir, "MainTest.class");
        mainClass.createNewFile();
        testClass.createNewFile();

        // Create include and exclude patterns
        PatternSet.NameEntry in = deleteTask.createInclude();
        in.setName("*.class");
        PatternSet.NameEntry ex = deleteTask.createExclude();
        ex.setName("*Test*");

        deleteTask.execute();

        assertFalse("Standard class files should be deleted", mainClass.exists());
        assertTrue("Test class files should be preserved", testClass.exists());
    }

    /*
     * Test Scenario 4: Case-Insensitive Deletion with Logical NOT selector
     *
     * Purpose: tests deleting files regardless of capitalization (setCaseSensitive) 
     * while using a logical NOT selector (addNot) to protect a specific file.
     */
    @Test
    public void testCaseInsensitiveAndNotSelector() throws IOException {
        File dir = tempFolder.newFolder("case");
        deleteTask.setDir(dir);

        File upper = new File(dir, "upper.TXT");
        File lower = new File(dir, "lower.txt");
        File protectedF = new File(dir, "protected.txt");
        
        upper.createNewFile();
        lower.createNewFile();
        protectedF.createNewFile();

        // Target 1: setCaseSensitive(false)
        deleteTask.setCaseSensitive(false);
        deleteTask.setIncludes("*.txt");

        // Target 2: addNot() to protect a specific file
        NotSelector nSelector = new NotSelector();
        FilenameSelector f1Selector = new FilenameSelector();
        f1Selector.setName("protected.txt");
        nSelector.appendSelector(f1Selector); 
        
        deleteTask.addNot(nSelector);
        deleteTask.execute();

        assertFalse("Uppercase TXT should be deleted due to case-insensitivity", upper.exists());
        assertFalse("Lowercase txt should be deleted", lower.exists());
        assertTrue("Protected file should be preserved due to NotSelector", protectedF.exists());
    }

    /*
     * Test Scenario 5: Cross-Directory Cleanup
     *
     * Purpose: tests cross-directory presence check (addPresent, addAnd). 
     * Deletes files in 'build' ONLY IF they also exist in 'src' directory.
     */
    @Test
    public void testCrossDirectoryPresenceCleanup() throws IOException {
        File srcDir = tempFolder.newFolder("src");
        File buildDir = tempFolder.newFolder("build");
        deleteTask.setDir(buildDir);

        File commonSrc = new File(srcDir, "Main.java");
        File commonBuild = new File(buildDir, "Main.java");
        commonSrc.createNewFile();
        commonBuild.createNewFile();

        File uniqueBuild = new File(buildDir, "Unique.java");
        uniqueBuild.createNewFile();

        // Target 1: addAnd() to group selectors
        AndSelector aSelector = new AndSelector();
        deleteTask.addAnd(aSelector);

        // Target 2: addPresent() - Check if equivalent file is present in srcDir
        PresentSelector pSelector = new PresentSelector();
        pSelector.setTargetdir(srcDir);
        
        // Identity mapper: name stays the same
        Mapper mapper = pSelector.createMapper(); 
        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity"); 
        mapper.setType(identity);
        
        deleteTask.addPresent(pSelector);
        deleteTask.execute();

        assertFalse("Common build file should be deleted", commonBuild.exists());
        assertTrue("Orphan build file should remain", uniqueBuild.exists());
    }

    /*
     * Test Scenario 6: Multi-Dimensional Filtering (Depth + Size + Or)
     *
     * Purpose: A complex integration test combinating addDepth, addSize, and addOr. 
     * Simulates a maintenance job that deletes files ONLY IF they meet three criteria:
     * 1. They are buried in a subdirectory (Depth >= 1) -> protects root files.
     * 2. They are small files (Size < 50 bytes) -> ignores empty placeholders.
     * 3. Their extension is EITHER "*.log" OR "*.tmp".
     */
    @Test
    public void testDeepSmallLogOrTmpCleanup() throws IOException {
        File dir = tempFolder.newFolder("logs");
        deleteTask.setDir(dir);

        File subDir = new File(dir, "archive");
        subDir.mkdir();

        // File 1: Depth = 0, size > 50 bytes
        File keep1 = new File(dir, "root.log");
        Files.write(keep1.toPath(), new byte[1000]); 

        // File 2: Depth = 1, size > 50 bytes
        File keep2 = new File(subDir, "big.tmp");
        Files.write(keep2.toPath(), new byte[1000]); 

        // File 3: Depth = 0, size < 50 bytes
        File keep3 = new File(dir, "rootSmall.log");
        Files.write(keep3.toPath(), new byte[10]); 

        // File 4: Depth = 1, size < 50 bytes but not "*.log" OR "*.tmp"
        File keep4 = new File(subDir, "small.txt");
        Files.write(keep4.toPath(), new byte[10]); 

        // File 5: Depth = 1, Size < 50, name = *.log (Match)
        File nokeep = new File(subDir, "crash.log");
        Files.write(nokeep.toPath(), new byte[10]); 

        // Target 1: addDepth() - Must be at least 1 directory deep
        DepthSelector dSelector = new DepthSelector();
        dSelector.setMin(1);
        deleteTask.addDepth(dSelector);

        // Target 2: addSize() - Must be smaller than 50 bytes
        SizeSelector sSelector = new SizeSelector();
        sSelector.setValue(50);
        SizeSelector.SizeComparisons sCondition = new SizeSelector.SizeComparisons();
        sCondition.setValue("less"); 
        sSelector.setWhen(sCondition);
        deleteTask.addSize(sSelector);

        // Target 3: addOr() - Must be "*.log" OR "*.tmp"
        OrSelector oSelector = new OrSelector();
        FilenameSelector f1Selector = new FilenameSelector();
        f1Selector.setName("**/*.log");
        FilenameSelector f2Selector = new FilenameSelector();
        f2Selector.setName("**/*.tmp");
        
        oSelector.appendSelector(f1Selector);
        oSelector.appendSelector(f2Selector);
        deleteTask.addOr(oSelector);

        // Execute
        deleteTask.execute();

        // Verify the results
        assertTrue("Big root log should be kept because it fails the Depth and Size condition", keep1.exists());
        assertTrue("Big tmp should be kept because it fails the Size condition", keep2.exists());
        assertTrue("Small root log should be kept because it fails the Depth condition", keep3.exists());//OR condition (.txt)
        assertTrue("Small txt should be kept because it fails the OR condition", keep4.exists());//
        
        assertFalse("Deep small log should be deleted because it passes ALL conditions", nokeep.exists());
    }
    
}