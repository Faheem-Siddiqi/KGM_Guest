package com.kgm.service;

public final class GuestIdentifierRules {
    private static final String PASSPORT_PATTERN = "[A-Za-z0-9]+([ -][A-Za-z0-9]+)*";

    private GuestIdentifierRules() {
    }

    public static boolean isCnicWithoutDashes(String value) {
        return text(value).matches("\\d{13}");
    }

    public static boolean isPassport(String value) {
        String text = text(value);
        String compact = compactIdentifier(text);
        return compact.length() >= 4
                && compact.length() <= 30
                && compact.matches(".*[A-Za-z].*")
                && text.matches(PASSPORT_PATTERN);
    }

    public static String compactIdentifier(String value) {
        return text(value)
                .replace("-", "")
                .replace(" ", "")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
