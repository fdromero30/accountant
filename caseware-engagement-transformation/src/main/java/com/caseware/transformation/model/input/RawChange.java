package com.caseware.transformation.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Locale;
import java.util.Optional;

/**
 * A single raw change entry inside a {@link RawTemplateDiff}.
 *
 * <p>The JSON shape follows an RFC&nbsp;6902 style document:</p>
 * <pre>{@code
 * { "op": "replace", "path": "/sections/materiality/guidance/thresholdPercent",
 *   "oldValue": 5.0, "newValue": 4.5 }
 * }</pre>
 *
 * <p>{@code add} operations carry their payload in {@code value}, {@code remove} operations carry
 * the deleted payload in {@code oldValue} and {@code replace} operations populate both
 * {@code oldValue} and {@code newValue}.</p>
 *
 * @param op       raw operation name ({@code add}, {@code replace} or {@code remove}); mandatory
 * @param path     JSON pointer of the affected template node; mandatory
 * @param oldValue value before a {@code replace}/{@code remove}, {@code null} otherwise
 * @param newValue value after a {@code replace}, {@code null} otherwise
 * @param value    payload of an {@code add} operation, {@code null} otherwise
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawChange(
        String op,
        String path,
        Object oldValue,
        Object newValue,
        Object value) {

    private static final String OP_ADD = "add";
    private static final String OP_REPLACE = "replace";
    private static final String OP_REMOVE = "remove";

    public RawChange {
        InputGuard.requireText(op, "op");
        InputGuard.requireText(path, "path");
    }

    public boolean isAdd() {
        return OP_ADD.equals(normalizedOp());
    }

    public boolean isReplace() {
        return OP_REPLACE.equals(normalizedOp());
    }

    public boolean isRemove() {
        return OP_REMOVE.equals(normalizedOp());
    }

    /**
     * @return the payload that was introduced by this change, if any ({@code value} for additions,
     *         {@code newValue} for replacements)
     */
    public Optional<Object> introducedValue() {
        if (isAdd()) {
            return Optional.ofNullable(value);
        }
        return Optional.ofNullable(newValue);
    }

    /**
     * @return the payload that was removed by this change, if any ({@code oldValue} for removals and
     *         replacements)
     */
    public Optional<Object> removedValue() {
        return Optional.ofNullable(oldValue);
    }

    private String normalizedOp() {
        return op.trim().toLowerCase(Locale.ROOT);
    }
}
