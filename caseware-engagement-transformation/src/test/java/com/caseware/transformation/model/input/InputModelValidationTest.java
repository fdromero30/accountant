package com.caseware.transformation.model.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the input contract: an engagement cannot be built without a template, every field is
 * mandatory and template versions start at {@code 1}.
 */
@DisplayName("Input model validation")
class InputModelValidationTest {

    @Test
    @DisplayName("an engagement cannot be created without a template")
    void rejectsEngagementWithoutTemplate() {
        assertThatThrownBy(() -> new Engagement("ENG-1001", "Northstar Manufacturing 2026", null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");
        assertThatThrownBy(() -> new Engagement("ENG-1001", "Northstar Manufacturing 2026", "  ", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");

        assertThat(new Engagement("ENG-1001", "Northstar Manufacturing 2026", "AUDIT-CA", 5).templateId())
                .isEqualTo("AUDIT-CA");
    }

    @Test
    @DisplayName("every engagement field is mandatory")
    void rejectsIncompleteEngagements() {
        assertThatThrownBy(() -> new Engagement(null, "Northstar Manufacturing 2026", "AUDIT-CA", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("engagementId");
        assertThatThrownBy(() -> new Engagement(" ", "Northstar Manufacturing 2026", "AUDIT-CA", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("engagementId");
        assertThatThrownBy(() -> new Engagement("ENG-1001", null, "AUDIT-CA", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("template versions start at 1")
    void rejectsVersionsBelowOne() {
        assertThatThrownBy(() -> new Engagement("ENG-1001", "Northstar Manufacturing 2026", "AUDIT-CA", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateVersion")
                .hasMessageContaining("at least 1");
        assertThatThrownBy(() -> new Engagement("ENG-1001", "Northstar Manufacturing 2026", "AUDIT-CA", -3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateVersion");

        assertThatThrownBy(() -> new Template("AUDIT-CA", "Canadian Audit Engagement", 0, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latestVersion");
        assertThatThrownBy(() -> new TemplateVersionInfo(0, "2026-08-18T13:00:00Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
        assertThatThrownBy(() -> new RawTemplateDiff("AUDIT-CA", 0, 5, "2026-08-18T13:04:41Z", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromVersion");
        assertThatThrownBy(() -> new RawTemplateDiff("AUDIT-CA", 4, 0, "2026-08-18T13:04:41Z", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toVersion");
    }

    @Test
    @DisplayName("every template field is mandatory")
    void rejectsIncompleteTemplates() {
        assertThatThrownBy(() -> new Template(null, "Canadian Audit Engagement", 5, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");
        assertThatThrownBy(() -> new Template("AUDIT-CA", "  ", 5, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
        assertThatThrownBy(() -> new TemplateVersionInfo(4, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publishedAt");
    }

    @Test
    @DisplayName("every diff and change field is mandatory")
    void rejectsIncompleteDiffsAndChanges() {
        assertThatThrownBy(() -> new RawTemplateDiff(null, 3, 4, "2026-07-07T13:02:18Z", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");
        assertThatThrownBy(() -> new RawTemplateDiff("AUDIT-CA", 3, 4, " ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("generatedAt");
        assertThatThrownBy(() -> new RawChange(null, "/sections/planning/questions/7", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("op");
        assertThatThrownBy(() -> new RawChange("add", "  ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path");
    }

    @Test
    @DisplayName("null collections are normalized to empty ones")
    void normalizesNullCollections() {
        assertThat(new Template("AUDIT-CA", "Canadian Audit Engagement", 5, null).versions()).isEmpty();
        assertThat(new RawTemplateDiff("AUDIT-CA", 3, 4, "2026-07-07T13:02:18Z", null).changes()).isEmpty();
    }
}
