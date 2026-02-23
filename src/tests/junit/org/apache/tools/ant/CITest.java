package org.apache.tools.ant;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CITest {
    @Test
    public void testForCI() {
        // Simple test to prove CI work on new test case
        assertEquals(2, 1 + 1); 
    }
}
