package com.kgm.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelSampleGeneratorTest {

    @Test
    void sampleTemplateUsesCnicPassportHeaderWithSpaces() {
        assertTrue(ExcelSampleGenerator.templateHeaders().contains("CNIC / Passport"));
        assertTrue(ExcelSampleGenerator.templateHeaderLine().contains("CNIC / Passport"));
        assertTrue(ExcelSampleGenerator.importGuideMessage().contains("CNIC / Passport is strict"));
        assertTrue(ExcelSampleGenerator.importGuideMessage().contains("Legacy/historical import may repeat old placeholder"));
        assertTrue(ExcelSampleGenerator.importGuideMessage().contains("Guest Name + exact Arrival Date Time + exact Departure Date Time + Guest Category"));
        assertTrue(ExcelSampleGenerator.importGuideMessage().contains("PX1234567"));
        assertTrue(ExcelSampleGenerator.importGuideMessage().contains("3520212345678"));
    }

    @Test
    void sampleRowsAlwaysIncludeTwoNonPakistaniPassportExamples() throws Exception {
        Method sampleRows = ExcelSampleGenerator.class.getDeclaredMethod("sampleRows", List.class, List.class);
        sampleRows.setAccessible(true);

        ExcelSampleGenerator.SampleAccommodation accommodation = new ExcelSampleGenerator.SampleAccommodation(
                "Guest Room",
                "Room-101",
                "Ready for Assignment",
                4,
                4
        );
        @SuppressWarnings("unchecked")
        List<String[]> rows = (List<String[]>) sampleRows.invoke(
                null,
                List.of(accommodation),
                List.of("Family")
        );

        long foreignRows = rows.stream()
                .filter(row -> !row[2].equalsIgnoreCase("Pakistan") && !row[2].equalsIgnoreCase("Pakistani"))
                .filter(row -> GuestIdentifierRules.isPassport(row[1]))
                .count();
        assertTrue(rows.size() >= 4);
        assertTrue(foreignRows >= 2);
    }

    @Test
    void validValuesSheetDocumentsStandardAndLegacyImportRules() throws Exception {
        Path file = Files.createTempFile("guest_import_sample_test", ".xlsx");
        try {
            ExcelSampleGenerator.writeSampleWorkbook(file.toFile());

            try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
                Sheet sheet = workbook.getSheet("Valid Values");
                assertNotNull(sheet);

                String sheetText = sheetText(sheet);
                assertTrue(sheetText.contains("Import Guide and Rules"));
                assertTrue(sheetText.contains("Import Rules and Checks"));
                assertTrue(sheetText.contains("CNIC / Passport Examples"));
                assertTrue(sheetText.contains("Date and Time Rules"));
                assertTrue(sheetText.contains("Current Valid Values From Database"));
                assertTrue(sheetText.indexOf("Current Valid Values From Database")
                        < sheetText.indexOf("Import Guide and Rules"));
                assertTrue(sheetText.contains("The importer reads the first worksheet only"));
                assertTrue(sheetText.contains("Legacy duplicate check against existing DB records uses Guest Name + exact Arrival Date Time + exact Departure Date Time + Guest Category"));
                assertTrue(sheetText.contains("CNIC / Passport does not need to be unique for legacy imports"));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static String sheetText(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        StringBuilder text = new StringBuilder();
        for (Row row : sheet) {
            for (Cell cell : row) {
                text.append(formatter.formatCellValue(cell)).append('\n');
            }
        }
        return text.toString();
    }
}
