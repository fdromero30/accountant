package com.caseware.transformation.model.output;

import java.util.List;

/**
 * Response payload backing the engagement list screen.
 *
 * @param totalCount          number of engagements contained in {@code engagements}
 * @param pendingUpdatesCount engagements that have at least one pending template change
 * @param engagements         transformed engagements; never {@code null}
 */
public record EngagementListResponse(
        int totalCount,
        int pendingUpdatesCount,
        List<EngagementItemDto> engagements) {

    public EngagementListResponse {
        engagements = engagements == null ? List.of() : List.copyOf(engagements);
    }
}
