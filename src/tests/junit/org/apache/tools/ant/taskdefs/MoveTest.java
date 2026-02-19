/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the Move task.
 *
 */
public class MoveTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/move.xml");
        buildRule.executeTarget("setUp");
    }

    // --- 1. Spy class (force rename failed) ---
    public static class MoveSpy extends Move {
        @Override
        protected boolean renameFile(File sourceFile, File destFile, boolean filtering, boolean overwrite) throws IOException {
            return false;
        }
    }

    // --- 2. Main ---
    @Test
    public void testMoveCoverageEnhancementCarol() throws Exception {

        File tmp = new File(buildRule.getProject().getProperty("output"));
        File srcDir = new File(tmp, "spy_src");
        File destDir = new File(tmp, "spy_dest");
        
        if (srcDir.exists()) { chmod(srcDir, "777"); forceDelete(srcDir); }
        if (destDir.exists()) { chmod(destDir, "777"); forceDelete(destDir); }
        srcDir.mkdirs();
        
        File f1 = new File(srcDir, "file1.txt");
        f1.createNewFile();
        File sub = new File(srcDir, "sub");
        sub.mkdirs();
        File f2 = new File(sub, "file2.txt");
        f2.createNewFile();

        // Spy Mission (renameFile & doFileOperations)
        MoveSpy spyMove = new MoveSpy();
        spyMove.setProject(buildRule.getProject());
        spyMove.setTaskName("move");
        spyMove.setFile(srcDir); // Move the whole table of contents
        spyMove.setTodir(destDir);
        spyMove.setIncludeEmptyDirs(true);
        spyMove.setOverwrite(true);
        spyMove.execute();

        // deleteDir
        File srcLocked = new File(tmp, "src_locked");
        srcLocked.mkdirs();
        File f3 = new File(srcLocked, "stuck.txt");
        f3.createNewFile();
        
        chmod(srcLocked, "000");

        MoveSpy failSpy = new MoveSpy();
        failSpy.setProject(buildRule.getProject());
        failSpy.setFile(f3);
        failSpy.setTodir(new File(destDir, "stuck_moved.txt"));
        failSpy.setPerformGcOnFailedDelete(true); // 觸發 GC
        failSpy.setFailOnError(false);
        
        try {
            failSpy.execute();
        } catch (Exception e) {
            // Expect Error
        } finally {
            chmod(srcLocked, "777");
            forceDelete(srcLocked);
        }
    }

    // 3. Auxiliary method: Modify permissions (For macOS/Linux)
    private void chmod(File f, String mode) throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            Runtime.getRuntime().exec("chmod " + mode + " " + f.getAbsolutePath()).waitFor();
        } else {
            f.setWritable(mode.contains("7"));
        }
    }

    // 4. Auxiliary method: Force deletion (resolves FileUtilities error issues)
    private void forceDelete(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        forceDelete(child);
                    }
                }
            }
            file.delete();
        }
    }

    @Test
    public void testFilterSet() throws IOException {
        buildRule.executeTarget("testFilterSet");
        File tmp  = new File(buildRule.getProject().getProperty("output"), "move.filterset.tmp");
        File check  = new File(buildRule.getProject().getBaseDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(check), FileUtilities.getFileContents(tmp));
    }

    @Test
    public void testFilterChain() throws IOException {
        buildRule.executeTarget("testFilterChain");
        File tmp  = new File(buildRule.getProject().getProperty("output"), "move.filterchain.tmp");
        File check  = new File(buildRule.getProject().getBaseDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(check), FileUtilities.getFileContents(tmp));
    }

    @Test
    public void testMoveFolderWithFilters() throws IOException {
        File srcDir = new File(buildRule.getProject().getProperty("output"), "move_src");
        File destDir = new File(buildRule.getProject().getProperty("output"), "move_dest");
        srcDir.mkdirs();
        new File(srcDir, "test.txt").createNewFile();

        Move move = new Move();
        move.setProject(buildRule.getProject());
        move.setTodir(destDir);
        
        // Add ResourceCollection and Filtering settings
        org.apache.tools.ant.types.FileSet fs = new org.apache.tools.ant.types.FileSet();
        fs.setDir(srcDir);
        move.addFileset(fs);
        
        move.setFiltering(true);
        move.setFlatten(true);
        move.setOverwrite(true);
        
        move.execute();
    }

    /** Bugzilla Report 11732 */
    @Test
    public void testDirectoryRemoval() {

        buildRule.executeTarget("testDirectoryRemoval");
        String output = buildRule.getProject().getProperty("output");
        assertFalse(new File(output, "E/B/1").exists());
        assertTrue(new File(output, "E/C/2").exists());
        assertTrue(new File(output, "E/D/3").exists());
        assertTrue(new File(output, "A/B/1").exists());
        assertFalse(new File(output, "A/C/2").exists());
        assertFalse(new File(output, "A/D/3").exists());
        assertFalse(new File(output, "A/C").exists());
        assertFalse(new File(output, "A/D").exists());
    }

    /** Bugzilla Report 18886 */
    @Test
    public void testDirectoryRetaining() {
        buildRule.executeTarget("testDirectoryRetaining");
        String output = buildRule.getProject().getProperty("output");
        assertTrue(new File(output, "E").exists());
        assertTrue(new File(output, "E/1").exists());
        assertFalse(new File(output, "A/1").exists());
        assertTrue(new File(output, "A").exists());
    }

    @Test
    public void testCompleteDirectoryMove() {
        testCompleteDirectoryMove("testCompleteDirectoryMove");
    }

    @Test
    public void testCompleteDirectoryMove2() {
        testCompleteDirectoryMove("testCompleteDirectoryMove2");
    }

    private void testCompleteDirectoryMove(String target) {
        buildRule.executeTarget(target);
        String output = buildRule.getProject().getProperty("output");
        assertTrue(new File(output, "E").exists());
        assertTrue(new File(output, "E/1").exists());
        assertFalse(new File(output, "A/1").exists());
        // <path> swallows the basedir, it seems
        //assertFalse(new File(getOutputDir(), "A").exists());
    }

    @Test
    public void testPathElementMove() {
        buildRule.executeTarget("testPathElementMove");
        String output = buildRule.getProject().getProperty("output");
        assertTrue(new File(output, "E").exists());
        assertTrue(new File(output, "E/1").exists());
        assertFalse(new File(output, "A/1").exists());
        assertTrue(new File(output, "A").exists());
    }

    @Test
    public void testMoveFileAndFileset() {
        buildRule.executeTarget("testMoveFileAndFileset");
    }

    @Test
    public void testCompleteDirectoryMoveToExistingDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveToExistingDir");
    }

    @Test
    public void testCompleteDirectoryMoveFileToFile() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToFile");
    }

    @Test
    public void testCompleteDirectoryMoveFileToDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToDir");
    }

    @Test
    public void testCompleteDirectoryMoveFileAndFileset() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileAndFileset");
    }

    @Test
    public void testCompleteDirectoryMoveFileToExistingFile() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToExistingFile");
    }

    @Test
    public void testCompleteDirectoryMoveFileToExistingDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToExistingDir");
    }

    @Test
    public void testCompleteDirectoryMoveFileToDirWithExistingFile() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToDirWithExistingFile");
    }

    @Test
    public void testCompleteDirectoryMoveFileToDirWithExistingDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToDirWithExistingDir");
    }

}
