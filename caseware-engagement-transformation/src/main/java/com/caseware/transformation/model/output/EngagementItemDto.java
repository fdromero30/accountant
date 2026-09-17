package com.caseware.transformation.model.output;

/**
 * One row of the engagement list view: the engagement plus the template update information the
 * user needs to decide whether an update is worth applying.
 *
 * @param engagementId        stable identifier of the engagement
 * @param name                human readable engagement name
 * @param templateId          identifier of the template the engagement is based on
 * @param templateDisplayName human readable template name
 * @param currentVersion      template version the engagement currently lives on
 * @param latestVersion       newest published version of the template
 * @param versionsBehind      number of published versions between {@code currentVersion} and
 *                            {@code latestVersion}; never negative
 * @param status              alignment of the engagement with the latest template version
 */
public record EngagementItemDto(
        String engagementId,
        String name,
        String templateId,
        String templateDisplayName,
        int currentVersion,
        int latestVersion,
        int versionsBehind,
        UpdateStatus status) {

    /**
     * @return {@code true} when the engagement has template changes waiting to be reviewed
     */
    public boolean hasPendingUpdates() {
        return status == UpdateStatus.PENDING_UPDATE;
    }
}
