package com.caseware.transformation.model.output;

/**
 * How much attention an engagement owner should pay to a pending template change.
 *
 * <p>Ordered from least to most disruptive, so callers can sort or filter on
 * {@link #ordinal()}.</p>
 */
public enum ImpactLevel {

    /** Cosmetic changes such as wording on a label: safe to apply automatically. */
    LOW,

    /** New or re-scoped content: the engagement has to be updated before it can be closed. */
    MEDIUM,

    /** Removed content: data already captured on the engagement may need remediation. */
    HIGH
}
