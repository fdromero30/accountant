package com.caseware.transformation.model.output;

/**
 * Whether an engagement is aligned with the latest published version of its template.
 *
 * <p>{@link #COMPUTING_SUMMARY} is reserved for callers that resolve the pending changes
 * asynchronously: the engagement list can already be rendered while the detailed, human readable
 * summary is still being computed. This synchronous service never produces it.</p>
 */
public enum UpdateStatus {

    /** The engagement already runs on the latest published template version. */
    UP_TO_DATE,

    /** A newer template version exists and at least one change has to be reviewed. */
    PENDING_UPDATE,

    /** The human readable summary is still being computed for the engagement. */
    COMPUTING_SUMMARY
}
