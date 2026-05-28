package com.kgm.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

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
}
