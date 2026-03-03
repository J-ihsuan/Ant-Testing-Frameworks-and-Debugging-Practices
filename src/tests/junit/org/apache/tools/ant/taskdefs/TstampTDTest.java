package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import static org.junit.Assert.assertEquals;

public class TstampTDTest {

    private Project project;

    @Before
    public void setUp() {
        project = new Project();
    }

    // ==========================================
    // Refactored Dummy Class (Testable Design)
    // ==========================================
    class TstampTestable {
        private Project project;
        
        public TstampTestable(Project project) { 
            this.project = project; 
        }
        
        // Dependency Injection via Method Parameter
        public void executeWithDate(Date fixedDate) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            // FIX: Force UTC timezone so the test passes regardless of the computer's geographical location!
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            project.setProperty("TODAY_DATE", sdf.format(fixedDate));
        }
    }

    // ==========================================
    // Test Case
    // ==========================================
    @Test
    public void testRefactoredTstampWithDeterministicDate() {
        // 1. Setup
        TstampTestable task = new TstampTestable(project);
        
        // 2. The Stub (Mocking the System Clock)
        // Set to Unix Epoch Time (1970-01-01 00:00:00 UTC)
        Date fixedDate = new Date(0); 
        
        // 3. Execute
        task.executeWithDate(fixedDate);
        
        // 4. Verification (100% Deterministic)
        assertEquals("19700101", project.getProperty("TODAY_DATE"));
    }
}