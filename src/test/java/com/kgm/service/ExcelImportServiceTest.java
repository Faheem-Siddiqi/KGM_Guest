package com.kgm.service;

import com.kgm.model.Guest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelImportServiceTest {

    @Test
    void cnicPassportHeaderAcceptsNewAndLegacyNames() throws Exception {
        ExcelImportService service = new ExcelImportService();
        Method canonicalHeader = ExcelImportService.class.getDeclaredMethod("canonicalHeader", String.class);
        canonicalHeader.setAccessible(true);

        assertEquals("CNIC / Passport", canonicalHeader.invoke(service, "CNIC / Passport"));
        assertEquals("CNIC / Passport", canonicalHeader.invoke(service, "CNIC/Passport"));
        assertEquals("CNIC / Passport", canonicalHeader.invoke(service, "Guest CNIC"));
        assertEquals("CNIC / Passport", canonicalHeader.invoke(service, "Passport"));
    }

    @Test
    void legacyIdentifierValidationAllowsRepeatedPlaceholderCnicAndStrictPassport() {
        assertDoesNotThrow(() -> validateLegacyIdentifier("9999999999999"));
        assertDoesNotThrow(() -> validateLegacyIdentifier("PX1234567"));
        assertDoesNotThrow(() -> validateLegacyIdentifier("AB-123 456"));

        assertThrows(Exception.class, () -> validateLegacyIdentifier("35202-1234567-8"));
        assertThrows(Exception.class, () -> validateLegacyIdentifier("AB/123456"));
        assertThrows(Exception.class, () -> validateLegacyIdentifier("AB--123456"));
        assertThrows(Exception.class, () -> validateLegacyIdentifier("1234567"));
    }

    private void validateLegacyIdentifier(String identifier) throws Exception {
        ExcelImportService service = new ExcelImportService();
        Method validateIdentifier = ExcelImportService.class.getDeclaredMethod("validateProvidedLegacyIdentifier", Guest.class);
        validateIdentifier.setAccessible(true);

        Guest guest = new Guest();
        guest.setCnic(identifier);
        try {
            validateIdentifier.invoke(service, guest);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checkedException) {
                throw checkedException;
            }
            throw new RuntimeException(cause);
        }
    }
}
