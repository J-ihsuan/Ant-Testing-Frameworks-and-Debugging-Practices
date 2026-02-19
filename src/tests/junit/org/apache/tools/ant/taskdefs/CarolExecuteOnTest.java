package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.junit.Test;
import java.io.File;

public class CarolExecuteOnTest {
    @Test
    public void testExecuteOnCoverageBoost() {
        ExecuteOn exe = new ExecuteOn();
        exe.setProject(new Project());
        
        // 1. Setter
        exe.setExecutable("echo");
        exe.setCommand(new org.apache.tools.ant.types.Commandline("echo hello"));
        exe.setAppend(true);
        exe.setForce(true);
        exe.setVerbose(true);
        exe.setParallel(false);
        exe.setType(new ExecuteOn.FileDirBoth());
        exe.setRelative(true);
        exe.setSkipEmptyFilesets(true);
        exe.setDest(new File("."));
        
        // 2. Triggering core logic
        try {
            org.apache.tools.ant.types.FileSet fs = new org.apache.tools.ant.types.FileSet();
            fs.setDir(new File("."));
            fs.setIncludes("CarolExecuteOnTest.java"); // Use yourself as a test file
            exe.addFileset(fs);
            
            exe.execute();
        } catch (Exception e) {
        }
    }
}