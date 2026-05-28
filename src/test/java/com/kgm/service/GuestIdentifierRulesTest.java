package com.kgm.service;

import com.kgm.model.Guest;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuestIdentifierRulesTest {

    @Test
    void cnicMustBeExactlyThirteenDigitsWithoutDashes() {
        assertTrue(GuestIdentifierRules.isCnicWithoutDashes("9999999999999"));
        assertTrue(GuestIdentifierRules.isCnicWithoutDashes("3520212345678"));

        assertFalse(GuestIdentifierRules.isCnicWithoutDashes("35202-1234567-8"));
        assertFalse(GuestIdentifierRules.isCnicWithoutDashes("352021234567"));
        assertFalse(GuestIdentifierRules.isCnicWithoutDashes("35202123456789"));
        assertFalse(GuestIdentifierRules.isCnicWithoutDashes("PX1234567"));
    }

    @Test
    void passportMustUseLettersDigitsAndSingleSeparatorsOnly() {
        assertTrue(GuestIdentifierRules.isPassport("PX1234567"));
        assertTrue(GuestIdentifierRules.isPassport("AB-123 456"));
        assertTrue(GuestIdentifierRules.isPassport("A" + "1".repeat(29)));

        assertFalse(GuestIdentifierRules.isPassport("1234567"));
        assertFalse(GuestIdentifierRules.isPassport("AB/12345"));
        assertFalse(GuestIdentifierRules.isPassport("AB__12345"));
        assertFalse(GuestIdentifierRules.isPassport("AB--12345"));
        assertFalse(GuestIdentifierRules.isPassport("AB  12345"));
        assertFalse(GuestIdentifierRules.isPassport("ABC-"));
        assertFalse(GuestIdentifierRules.isPassport("A" + "1".repeat(30)));
    }

    @Test
    void compactIdentifierRemovesFrontEndSeparatorsBeforeStorage() {
        assertEquals("3520212345678", GuestIdentifierRules.compactIdentifier("35202-1234567-8"));
        assertEquals("AB123456", GuestIdentifierRules.compactIdentifier("ab-123 456"));
    }

    @Test
    void standardGuestValidationUsesNationalitySpecificIdentifierRules() throws SQLException {
        GuestValidationService service = new GuestValidationService();

        Guest pakistaniGuest = baseGuest("Pakistani", "3520212345678");
        assertTrue(service.validateStandardGuest(pakistaniGuest).fieldIssues().isEmpty());

        Guest foreignGuest = baseGuest("Turkish", "PX1234567");
        assertTrue(service.validateStandardGuest(foreignGuest).fieldIssues().isEmpty());

        Guest foreignGuestWithNumericPassport = baseGuest("Turkish", "1234567");
        assertFalse(service.validateStandardGuest(foreignGuestWithNumericPassport).fieldIssues().isEmpty());
    }

    private Guest baseGuest(String nationality, String identifier) {
        Guest guest = new Guest();
        guest.setGuestName("Test Guest");
        guest.setCnic(identifier);
        guest.setNationality(nationality);
        guest.setGuestCategory("Family");
        guest.setCompanyName("Test Company");
        guest.setVisitType("Official Visit");
        guest.setAddress("Test Address");
        guest.setRequestedBy("Requester");
        guest.setRequestedDepartment("Admin");
        guest.setApprovedBy("Approver");
        guest.setAccommodatedBy("Admin Office");
        guest.setArrivalAt(Date.from(Instant.parse("2026-01-01T09:00:00Z")));
        guest.setDepartureAt(Date.from(Instant.parse("2026-01-02T09:00:00Z")));
        guest.setAccommodation("");
        guest.setRoomName("");
        return guest;
    }
}
