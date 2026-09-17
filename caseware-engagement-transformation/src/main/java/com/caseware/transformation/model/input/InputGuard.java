package com.caseware.transformation.model.input;

/**
 * Validation shared by the input records.
 *
 * <p>Every input field is mandatory and template versions start at {@code 1}, so the rest of the
 * library never has to deal with a missing identifier or a "before the first version" sentinel.</p>
 */
final class InputGuard {

    private InputGuard() {
    }

    /**
     * @param value text to validate
     * @param field name of the field, used in the error message
     * @return the validated text
     * @throws IllegalArgumentException when the text is {@code null} or blank
     */
    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is mandatory");
        }
        return value;
    }

    /**
     * @param version version number to validate
     * @param field   name of the field, used in the error message
     * @return the validated version number
     * @throws IllegalArgumentException when the version is lower than {@code 1}
     */
    static int requireVersion(int version, String field) {
        if (version < 1) {
            throw new IllegalArgumentException(field + " must be at least 1 but was " + version);
        }
        return version;
    }
}
