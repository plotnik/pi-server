package io.plotnik.piserver.freewriting;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

public class HomeFolderTest {

    @Test
    void testFolder() throws FwException {
        String freewritingPath = "/Users/eabramovich/Documents/pi/fw/pages/";
        //Freewriting freewriting = new Freewriting(new File(freewritingPath));
        assertNotNull(freewritingPath);
    }  
}
