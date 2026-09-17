package com.caseware.transformation.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A client engagement that is pinned to a specific version of a template.
 *
 * <p>Mirrors the raw payload found in {@code fixtures/engagements.json}.</p>
 *
 * <p>Every field is mandatory: an engagement cannot be created without a template, so
 * {@code templateId} is always present in the input. Template versions start at {@code 1}, which means
 * {@code templateVersion} is never lower than {@code 1} and there is no "before the first version"
 * sentinel to handle.</p>
 *
 * @param engagementId     stable identifier of the engagement (e.g. {@code ENG-1001})
 * @param name             human readable engagement name
 * @param templateId       identifier of the template the engagement was created from (e.g. {@code AUDIT-CA});
 *                         mandatory
 * @param templateVersion  template version the engagement currently lives on; always {@code >= 1}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Engagement(
        String engagementId,
        String name,
        String templateId,
        int templateVersion) {

    public Engagement {
        InputGuard.requireText(engagementId, "engagementId");
        InputGuard.requireText(name, "name");
        InputGuard.requireText(templateId, "templateId");
        InputGuard.requireVersion(templateVersion, "templateVersion");
    }
}
