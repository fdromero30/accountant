package com.caseware.transformation.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A published diff between two consecutive (or, occasionally, non consecutive) versions of a template.
 *
 * <p>Mirrors the payload found in {@code fixtures/diffs/template-diff-*.json}.</p>
 *
 * @param templateId  identifier of the template the diff belongs to (e.g. {@code AUDIT-CA})
 * @param fromVersion source template version
 * @param toVersion   target template version
 * @param generatedAt ISO-8601 publication timestamp of the diff
 * @param changes     ordered list of raw changes; never {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawTemplateDiff(
        String templateId,
        int fromVersion,
        int toVersion,
        String generatedAt,
        List<RawChange> changes) {

    public RawTemplateDiff {
        InputGuard.requireText(templateId, "templateId");
        InputGuard.requireVersion(fromVersion, "fromVersion");
        InputGuard.requireVersion(toVersion, "toVersion");
        InputGuard.requireText(generatedAt, "generatedAt");
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    /**
     * @return {@code true} when {@code fromVersion} is strictly lower than {@code toVersion}
     */
    public boolean isForwardDiff() {
        return fromVersion < toVersion;
    }
}
