package com.caseware.transformation.model.output;

import java.time.Instant;
import java.util.List;

/**
 * Detailed view of the template updates waiting for a single engagement.
 *
 * @param engagementId        stable identifier of the engagement
 * @param engagementName      human readable engagement name
 * @param templateId          identifier of the template the engagement is based on
 * @param templateDisplayName human readable template name
 * @param currentVersion      template version the engagement currently lives on
 * @param targetVersion       template version the engagement would be upgraded to
 * @param accumulatedVersions versions whose diffs were accumulated to describe the upgrade, i.e.
 *                            the target version of every applied diff; empty when there is nothing
 *                            pending
 * @param status              alignment of the engagement with the latest template version
 * @param computedAt          ISO-8601 instant at which the summary was produced
 * @param totalChangesCount   number of changes contained in {@code changes}
 * @param changes             the human readable changes; never {@code null}
 */
public record PendingUpdateSummaryResponse(
        String engagementId,
        String engagementName,
        String templateId,
        String templateDisplayName,
        int currentVersion,
        int targetVersion,
        List<Integer> accumulatedVersions,
        UpdateStatus status,
        String computedAt,
        int totalChangesCount,
        List<HumanReadableChange> changes) {

    public PendingUpdateSummaryResponse {
        accumulatedVersions = accumulatedVersions == null ? List.of() : List.copyOf(accumulatedVersions);
        changes = changes == null ? List.of() : List.copyOf(changes);
    }
}
