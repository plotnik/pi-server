package io.plotnik.piserver.freewriting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.plotnik.piserver.freewriting.dao.FwDate;

@ExtendWith(MockitoExtension.class)
class FreewritingTest {

    @Mock
    private File mockHomeDir;
    
    @Mock
    private File mockDir;
    
    @Mock
    private File mockFile1, mockFile2, mockFile3;

    private Freewriting freewriting;
    
    @BeforeEach
    void setUp() throws Exception {
        // Setup mock behavior
        when(mockHomeDir.listFiles()).thenReturn(new File[]{mockDir});
        
        // Home folder contains subfolder
        when(mockDir.getName()).thenReturn("2016-осень");
        when(mockDir.isDirectory()).thenReturn(true);
        when(mockDir.listFiles()).thenReturn(new File[]{mockFile1, mockFile2, mockFile3});
        
        // Subfolder contains 3 files
        when(mockFile1.getName()).thenReturn("13 сентября вт.md");
        when(mockFile2.getName()).thenReturn("14 сентября ср.md");
        when(mockFile3.getName()).thenReturn("15 сентября чт.md");
        
        when(mockFile1.isDirectory()).thenReturn(false);
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile3.isDirectory()).thenReturn(false);

        // Initialize Freewriting object
        freewriting = new Freewriting(mockHomeDir);
    }
    
    @Test
    void testConstructor() throws FwException {
        assertNotNull(freewriting);
        assertNotNull(freewriting.today);
        assertEquals(LocalDate.parse("2016-09-13"), freewriting.dbStart);
    }
    
    @Test
    void testScanFolder() throws Exception {
        // This test verifies that the scanFolder method correctly processes files
        List<FwDate> dates = freewriting.getFWDates();
        assertEquals(3, dates.size());
        assertEquals(LocalDate.parse("2016-09-13"), dates.get(0).getDate());
        assertEquals(LocalDate.parse("2016-09-14"), dates.get(1).getDate());
        assertEquals(LocalDate.parse("2016-09-15"), dates.get(2).getDate());
    }
    
    @Test
    void testHandleDecember() {
        // Test for non-December month
        assertEquals(2024, freewriting.handleDecember(2024, 5, 6));

        // Test for December in a season folder
        assertEquals(2023, freewriting.handleDecember(2024, 11, -1));

        // Test for December in root folder (current month is December)
        assertEquals(2024, freewriting.handleDecember(2024, 11, 12));

        // Test for December in root folder (current month is not December)
        assertEquals(2023, freewriting.handleDecember(2024, 11, 1));
    }
    
    @Test
    void testNameFormat() {
        LocalDate date = LocalDate.parse("2024-05-13");
        assertEquals("13 мая пн", Freewriting.nameFormat(date));
    }    
    
    @Test
    void testExtractSeasonYear() {
        assertEquals(2024, freewriting.extractSeasonYear("2024-осень"));
    }
    
    @Test
    void testInvalidFileName() {
        when(mockFile1.getName()).thenReturn("invalid_file_name.md");
        assertThrows(FwException.class, () -> new Freewriting(mockHomeDir));
    }
}