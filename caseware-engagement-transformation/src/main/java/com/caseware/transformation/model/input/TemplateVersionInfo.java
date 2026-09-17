package com.caseware.transformation.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Publication metadata of a single template version.
 *
 * @param version     the template version number; template versions start at {@code 1}
 * @param publishedAt ISO-8601 timestamp at which the version became available to engagements
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateVersionInfo(
        int version,
        String publishedAt) {

    public TemplateVersionInfo {
        InputGuard.requireVersion(version, "version");
        InputGuard.requireText(publishedAt, "publishedAt");
    }
}
