package com.caseware.transformation.model.output;

/**
 * A single template change expressed for a non technical audience.
 *
 * @param id                deterministic identifier of the change within its response: the JSON
 *                          pointer of the affected node, suffixed with {@code #2}, {@code #3} and
 *                          so on when the same node is touched more than once
 * @param sectionKey        key of the template section the change belongs to
 *                          (e.g. {@code planning}); {@code null} for changes outside
 *                          {@code /sections}
 * @param sectionDisplayName human readable section name (e.g. {@code Planning}); {@code null} when
 *                          the change is not scoped to a section
 * @param changeType         normalized kind of change
 * @param title              short headline of the change, taken from the affected label when the
 *                           payload carries one
 * @param description        plain language description of the change
 * @param impactLevel        how disruptive the change is for the engagement
 * @param actionRequired     whether the accountant has to act on the change, i.e. whether the
 *                           impact is anything but {@link ImpactLevel#LOW}
 * @param oldValueSummary    readable summary of the value before the change; {@code null} when not
 *                           applicable
 * @param newValueSummary    readable summary of the value after the change; {@code null} when not
 *                           applicable
 */
public record HumanReadableChange(
        String id,
        String sectionKey,
        String sectionDisplayName,
        ChangeType changeType,
        String title,
        String description,
        ImpactLevel impactLevel,
        boolean actionRequired,
        String oldValueSummary,
        String newValueSummary) {
}
