package com.caseware.transformation.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A template catalog entry with its published versions.
 *
 * <p>Mirrors a single entry of {@code fixtures/templates.json}.</p>
 *
 * <p>The catalog is the authoritative list of templates: template identifiers are mandatory and
 * {@code latestVersion} is always at least {@code 1}, because template versions are numbered from
 * {@code 1} upwards.</p>
 *
 * @param templateId    identifier of the template (e.g. {@code AUDIT-CA}); mandatory
 * @param displayName   human readable template name
 * @param latestVersion newest published version; always {@code >= 1}
 * @param versions      published versions, ordered from oldest to newest; never {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Template(
        String templateId,
        String displayName,
        int latestVersion,
        List<TemplateVersionInfo> versions) {

    public Template {
        InputGuard.requireText(templateId, "templateId");
        InputGuard.requireText(displayName, "displayName");
        InputGuard.requireVersion(latestVersion, "latestVersion");
        versions = versions == null ? List.of() : List.copyOf(versions);
    }
}
