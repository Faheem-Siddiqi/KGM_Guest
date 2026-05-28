package com.kgm.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnvironmentConfig {
    private static final String ENV_FILE_NAME = ".env";
    private static Map<String, String> fileValues;

    private EnvironmentConfig() {
    }

    public static String setting(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }

        String environmentValue = System.getenv(envName);
        if (hasText(environmentValue)) {
            return environmentValue.trim();
        }

        String fileValue = envFileValues().get(envName);
        if (hasText(fileValue)) {
            return fileValue.trim();
        }

        return defaultValue;
    }

    private static synchronized Map<String, String> envFileValues() {
        if (fileValues != null) {
            return fileValues;
        }

        Map<String, String> values = new HashMap<>();
        Path envFile = findEnvFile();
        if (envFile != null) {
            try {
                for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                    addEnvLine(values, line);
                }
            } catch (IOException ignored) {
            }
        }
        fileValues = values;
        return fileValues;
    }

    private static Path findEnvFile() {
        Path directory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve(ENV_FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        return null;
    }

    private static void addEnvLine(Map<String, String> values, String line) {
        String text = line == null ? "" : line.trim();
        if (text.isEmpty() || text.startsWith("#")) {
            return;
        }
        if (text.startsWith("export ")) {
            text = text.substring("export ".length()).trim();
        }
        int separator = text.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = text.substring(0, separator).trim();
        String value = text.substring(separator + 1).trim();
        if (key.isEmpty()) {
            return;
        }
        values.put(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }
        List<String> quotePairs = List.of("\"\"", "''");
        String firstAndLast = value.substring(0, 1) + value.substring(value.length() - 1);
        if (!quotePairs.contains(firstAndLast)) {
            return value;
        }
        return value.substring(1, value.length() - 1);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
