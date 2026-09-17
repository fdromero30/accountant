package com.caseware.transformation.model.output;

import java.util.Locale;

/**
 * Normalized kind of change, mapped from the raw diff operations used by the publisher.
 */
public enum ChangeType {

    /** Content that exists in the newer template version but not in the older one. */
    ADDED,

    /** Content whose value changed between the two template versions. */
    MODIFIED,

    /** Content that existed in the older template version and was dropped. */
    REMOVED;

    /**
     * Maps a raw diff operation name to a {@link ChangeType}.
     *
     * @param operation raw operation name, e.g. {@code add}, {@code replace} or {@code remove}
     * @return the matching change type
     * @throws IllegalArgumentException when the operation is {@code null} or not supported
     */
    public static ChangeType fromOperation(String operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Diff operation must not be null");
        }
        return switch (operation.trim().toLowerCase(Locale.ROOT)) {
            case "add", "added" -> ADDED;
            case "remove", "removed", "delete", "deleted" -> REMOVED;
            case "replace", "modify", "modified", "update", "updated" -> MODIFIED;
            default -> throw new IllegalArgumentException("Unsupported diff operation: " + operation);
        };
    }
}
