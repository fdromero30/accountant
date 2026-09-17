package com.caseware.transformation.service;

import com.caseware.transformation.model.input.Engagement;
import com.caseware.transformation.model.input.RawChange;
import com.caseware.transformation.model.input.RawTemplateDiff;
import com.caseware.transformation.model.input.Template;
import com.caseware.transformation.model.input.TemplateVersionInfo;
import com.caseware.transformation.model.output.ChangeType;
import com.caseware.transformation.model.output.EngagementItemDto;
import com.caseware.transformation.model.output.EngagementListResponse;
import com.caseware.transformation.model.output.HumanReadableChange;
import com.caseware.transformation.model.output.ImpactLevel;
import com.caseware.transformation.model.output.PendingUpdateSummaryResponse;
import com.caseware.transformation.model.output.UpdateStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("EngagementTransformationService")
class EngagementTransformationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-16T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private static final String ENGAGEMENTS_FIXTURE = "fixtures/engagements.json";
    private static final String TEMPLATES_FIXTURE = "fixtures/templates.json";

    private static final List<String> DIFF_FIXTURES = List.of(
            "fixtures/diffs/template-diff-audit-ca-v3-v4.json",
            "fixtures/diffs/template-diff-audit-ca-v4-v5.json",
            "fixtures/diffs/template-diff-review-ca-v6-v7.json",
            "fixtures/diffs/template-diff-review-ca-v6-v8.json",
            "fixtures/diffs/template-diff-review-ca-v7-v8.json",
            "fixtures/diffs/template-diff-risk-ca-v10-v11.json",
            "fixtures/diffs/template-diff-risk-ca-v11-v12.json");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private static List<Engagement> engagements;
    private static List<Template> templates;
    private static List<RawTemplateDiff> diffs;
    private static EngagementTransformationService service;

    @BeforeAll
    static void loadFixtures() throws IOException {
        engagements = readFixture(ENGAGEMENTS_FIXTURE, new TypeReference<List<Engagement>>() { });
        templates = readFixture(TEMPLATES_FIXTURE, new TypeReference<List<Template>>() { });

        List<RawTemplateDiff> loaded = new ArrayList<>();
        for (String fixture : DIFF_FIXTURES) {
            loaded.add(readFixture(fixture, new TypeReference<RawTemplateDiff>() { }));
        }
        diffs = List.copyOf(loaded);
        service = new EngagementTransformationService(templates, diffs, FIXED_CLOCK);
    }

    private static <T> T readFixture(String classpathResource, TypeReference<T> type) throws IOException {
        try (InputStream input = EngagementTransformationServiceTest.class
                .getClassLoader().getResourceAsStream(classpathResource)) {
            assertThat(input)
                    .as("Expected fixture %s on the test classpath", classpathResource)
                    .isNotNull();
            return MAPPER.readValue(input, type);
        }
    }

    private static Engagement engagementOf(String engagementId) {
        return engagements.stream()
                .filter(engagement -> engagementId.equals(engagement.engagementId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No engagement " + engagementId + " in the fixtures"));
    }

    private static PendingUpdateSummaryResponse summaryOf(String engagementId) {
        return service.buildPendingUpdateSummary(engagementOf(engagementId));
    }

    private static HumanReadableChange changeAt(String engagementId, String changeId) {
        return summaryOf(engagementId).changes().stream()
                .filter(change -> changeId.equals(change.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No change " + changeId + " in " + engagementId));
    }

    private static EngagementTransformationService serviceWithout(Predicate<RawTemplateDiff> excluded) {
        return new EngagementTransformationService(
                templates,
                diffs.stream().filter(excluded.negate()).toList(),
                FIXED_CLOCK);
    }

    private static Map<String, Object> payload(Object... keyValuePairs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index < keyValuePairs.length; index += 2) {
            payload.put((String) keyValuePairs[index], keyValuePairs[index + 1]);
        }
        return payload;
    }

    @Test
    @DisplayName("loads the catalog, the engagements and the diffs from the JSON fixtures")
    void loadsFixturesFromTheClasspath() {
        assertThat(templates).extracting(Template::templateId)
                .containsExactly("AUDIT-CA", "REVIEW-CA", "RISK-CA");
        assertThat(engagements).hasSize(12);
        assertThat(diffs).hasSize(DIFF_FIXTURES.size());

        assertThat(templates.get(0).versions())
                .extracting(TemplateVersionInfo::publishedAt)
                .containsExactly("2026-05-12T13:00:00Z", "2026-07-07T13:00:00Z", "2026-08-18T13:00:00Z");
        assertThat(diffs.get(0).generatedAt()).isEqualTo("2026-07-07T13:02:18Z");
    }

    @Test
    @DisplayName("reads addition payloads from value and removal payloads from oldValue")
    void readsPayloadsFromRawChanges() {
        List<RawChange> changes = diffs.get(0).changes();

        assertThat(changes.get(0).isAdd()).isTrue();
        assertThat(changes.get(0).introducedValue()).get().isInstanceOf(Map.class);
        assertThat(changes.get(0).removedValue()).isEmpty();

        assertThat(changes.get(1).isReplace()).isTrue();
        assertThat(changes.get(1).removedValue()).hasValue(5.0);
        assertThat(changes.get(1).introducedValue()).hasValue(4.5);

        assertThat(changes.get(2).isRemove()).isTrue();
        assertThat(changes.get(2).removedValue()).get()
                .isEqualTo(payload("id", "PROC-PLN-004", "label", "Confirm legacy risk classification", "required", false));
        assertThat(changes.get(2).introducedValue()).isEmpty();
    }

    @Test
    @DisplayName("produces one item per engagement, preserving the input order")
    void transformsEveryEngagementInInputOrder() {
        EngagementListResponse response = service.buildEngagementList(engagements);

        assertThat(response.totalCount()).isEqualTo(12);
        assertThat(response.engagements()).extracting(EngagementItemDto::engagementId)
                .containsExactly("ENG-1001", "ENG-1002", "ENG-1003", "ENG-1004", "ENG-1005", "ENG-1006",
                        "ENG-1007", "ENG-1008", "ENG-1009", "ENG-1010", "ENG-1011", "ENG-1012");
    }

    @Test
    @DisplayName("counts the engagements that have template changes waiting")
    void countsEngagementsWithPendingUpdates() {
        EngagementListResponse response = service.buildEngagementList(engagements);

        assertThat(response.pendingUpdatesCount()).isEqualTo(6);
        assertThat(response.engagements()).filteredOn(EngagementItemDto::hasPendingUpdates)
                .extracting(EngagementItemDto::engagementId)
                .containsExactly("ENG-1002", "ENG-1003", "ENG-1006", "ENG-1007", "ENG-1010", "ENG-1011");
    }

    @Test
    @DisplayName("reports how many versions each engagement is behind and its update status")
    void reportsVersionsBehindAndStatus() {
        assertThat(service.buildEngagementList(engagements).engagements())
                .extracting(EngagementItemDto::engagementId,
                        EngagementItemDto::templateDisplayName,
                        EngagementItemDto::currentVersion,
                        EngagementItemDto::latestVersion,
                        EngagementItemDto::versionsBehind,
                        EngagementItemDto::status)
                .containsExactly(
                        tuple("ENG-1001", "Canadian Audit Engagement", 5, 5, 0, UpdateStatus.UP_TO_DATE),
                        tuple("ENG-1002", "Canadian Audit Engagement", 4, 5, 1, UpdateStatus.PENDING_UPDATE),
                        tuple("ENG-1003", "Canadian Audit Engagement", 3, 5, 2, UpdateStatus.PENDING_UPDATE),
                        tuple("ENG-1004", "Canadian Audit Engagement", 5, 5, 0, UpdateStatus.UP_TO_DATE),
                        tuple("ENG-1005", "Canadian Review Engagement", 8, 8, 0, UpdateStatus.UP_TO_DATE),
                        tuple("ENG-1006", "Canadian Review Engagement", 7, 8, 1, UpdateStatus.PENDING_UPDATE),
                        tuple("ENG-1007", "Canadian Review Engagement", 6, 8, 2, UpdateStatus.PENDING_UPDATE),
                        tuple("ENG-1008", "Canadian Review Engagement", 8, 8, 0, UpdateStatus.UP_TO_DATE),
                        tuple("ENG-1009", "Canadian Risk Assessment", 12, 12, 0, UpdateStatus.UP_TO_DATE),
                        tuple("ENG-1010", "Canadian Risk Assessment", 11, 12, 1, UpdateStatus.PENDING_UPDATE),
                        tuple("ENG-1011", "Canadian Risk Assessment", 10, 12, 2, UpdateStatus.PENDING_UPDATE),
                        tuple("ENG-1012", "Canadian Risk Assessment", 12, 12, 0, UpdateStatus.UP_TO_DATE));
    }

    @Test
    @DisplayName("never reports a negative number of versions behind")
    void clampsVersionsBehindForEngagementsAheadOfTheCatalog() {
        Engagement ahead = new Engagement("ENG-9999", "Future engagement", "AUDIT-CA", 7);

        EngagementItemDto item = service.transform(ahead);

        assertThat(item.currentVersion()).isEqualTo(7);
        assertThat(item.latestVersion()).isEqualTo(5);
        assertThat(item.versionsBehind()).isZero();
        assertThat(item.status()).isEqualTo(UpdateStatus.UP_TO_DATE);
        assertThat(item.hasPendingUpdates()).isFalse();

        PendingUpdateSummaryResponse summary = service.buildPendingUpdateSummary(ahead);
        assertThat(summary.targetVersion()).isEqualTo(5);
        assertThat(summary.accumulatedVersions()).isEmpty();
        assertThat(summary.changes()).isEmpty();
    }

    @Test
    @DisplayName("matches template identifiers ignoring case and surrounding blanks")
    void resolvesTemplateIdsIgnoringCaseAndBlanks() {
        EngagementItemDto item = service.transform(new Engagement("ENG-8888", "Padded", " audit-ca ", 4));

        assertThat(item.templateId()).isEqualTo("AUDIT-CA");
        assertThat(item.templateDisplayName()).isEqualTo("Canadian Audit Engagement");
        assertThat(item.versionsBehind()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails fast when an engagement references a template missing from the catalog")
    void failsFastOnEngagementWithoutCatalogTemplate() {
        Engagement orphan = new Engagement("ENG-7777", "Orphaned engagement", "TAX-CA", 1);

        assertThatThrownBy(() -> service.transform(orphan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TAX-CA")
                .hasMessageContaining("ENG-7777");
        assertThatThrownBy(() -> service.buildEngagementList(List.of(orphan)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buildPendingUpdateSummary(orphan))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects two diffs published for the same template route")
    void rejectsDuplicateDiffsForTheSameRoute() {
        RawTemplateDiff first = diffs.get(0);
        RawTemplateDiff duplicate = new RawTemplateDiff(first.templateId(), first.fromVersion(),
                first.toVersion(), first.generatedAt(), first.changes());

        assertThatThrownBy(() -> new EngagementTransformationService(templates, List.of(first, duplicate), FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate diff")
                .hasMessageContaining("AUDIT-CA");
    }

    @Test
    @DisplayName("prefers a directly published diff over chained consecutive diffs")
    void prefersDirectlyPublishedDiff() {
        PendingUpdateSummaryResponse summary = summaryOf("ENG-1007");

        assertThat(summary.engagementId()).isEqualTo("ENG-1007");
        assertThat(summary.engagementName()).isEqualTo("Bluewater Hospitality 2026");
        assertThat(summary.templateId()).isEqualTo("REVIEW-CA");
        assertThat(summary.currentVersion()).isEqualTo(6);
        assertThat(summary.targetVersion()).isEqualTo(8);
        assertThat(summary.status()).isEqualTo(UpdateStatus.PENDING_UPDATE);
        assertThat(summary.accumulatedVersions()).containsExactly(8);
        assertThat(summary.totalChangesCount()).isEqualTo(5);
        assertThat(summary.changes()).extracting(HumanReadableChange::id).containsExactly(
                "/metadata/displayName",
                "/sections/inquiries/questions/12",
                "/sections/analytics/procedures/2/tolerance",
                "/sections/inquiries/questions/4/helpText",
                "/sections/completion/checklists/going-concern");
    }

    @Test
    @DisplayName("chains consecutive diffs and suffixes duplicated JSON pointers")
    void chainsConsecutiveDiffsAndSuffixesDuplicatePointers() {
        PendingUpdateSummaryResponse summary = summaryOf("ENG-1003");

        assertThat(summary.currentVersion()).isEqualTo(3);
        assertThat(summary.targetVersion()).isEqualTo(5);
        assertThat(summary.accumulatedVersions()).containsExactly(4, 5);
        assertThat(summary.totalChangesCount()).isEqualTo(6);
        assertThat(summary.changes()).extracting(HumanReadableChange::id).containsExactly(
                "/sections/planning/questions/7",
                "/sections/materiality/guidance/thresholdPercent",
                "/sections/planning/procedures/legacy-risk-confirmation",
                "/sections/planning/questions/3/label",
                "/sections/materiality/guidance/thresholdPercent#2",
                "/sections/completion/checklists/subsequent-events");
        assertThat(summary.changes()).extracting(HumanReadableChange::id).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("exposes the target version of every applied diff")
    void exposesAccumulatedTargetVersions() {
        assertThat(service.resolveAccumulatedVersions("AUDIT-CA", 4, 5)).containsExactly(5);
        assertThat(service.resolveAccumulatedVersions("AUDIT-CA", 3, 5)).containsExactly(4, 5);
        assertThat(service.resolveAccumulatedVersions("REVIEW-CA", 6, 8)).containsExactly(8);
        assertThat(service.resolveAccumulatedVersions("RISK-CA", 10, 12)).containsExactly(11, 12);
        assertThat(service.resolveAccumulatedVersions("AUDIT-CA", 5, 5)).isEmpty();
    }

    @Test
    @DisplayName("aggregates every pending change of the engagement list")
    void aggregatesPendingChanges() {
        List<HumanReadableChange> changes = engagements.stream()
                .map(service::buildPendingUpdateSummary)
                .flatMap(summary -> summary.changes().stream())
                .toList();

        assertThat(changes).hasSize(26);
        assertThat(changes).filteredOn(change -> change.impactLevel() == ImpactLevel.HIGH).hasSize(3);
        assertThat(changes).filteredOn(change -> change.impactLevel() == ImpactLevel.MEDIUM).hasSize(20);
        assertThat(changes).filteredOn(change -> change.impactLevel() == ImpactLevel.LOW).hasSize(3);
        assertThat(changes).allSatisfy(change -> assertThat(change.actionRequired())
                .isEqualTo(change.impactLevel() != ImpactLevel.LOW));
    }

    @Test
    @DisplayName("classifies added, modified and removed content by impact")
    void classifiesChangesByImpact() {
        PendingUpdateSummaryResponse summary = summaryOf("ENG-1003");

        assertThat(summary.changes())
                .extracting(HumanReadableChange::changeType,
                        HumanReadableChange::impactLevel,
                        HumanReadableChange::actionRequired)
                .containsExactly(
                        tuple(ChangeType.ADDED, ImpactLevel.MEDIUM, true),
                        tuple(ChangeType.MODIFIED, ImpactLevel.MEDIUM, true),
                        tuple(ChangeType.REMOVED, ImpactLevel.HIGH, true),
                        tuple(ChangeType.MODIFIED, ImpactLevel.LOW, false),
                        tuple(ChangeType.MODIFIED, ImpactLevel.MEDIUM, true),
                        tuple(ChangeType.ADDED, ImpactLevel.MEDIUM, true));
    }

    @Test
    @DisplayName("treats a changed label as a cosmetic, low impact change")
    void treatsLabelOnlyChangesAsCosmetic() {
        HumanReadableChange change = changeAt("ENG-1002", "/sections/planning/questions/3/label");

        assertThat(change.changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(change.impactLevel()).isEqualTo(ImpactLevel.LOW);
        assertThat(change.actionRequired()).isFalse();
        assertThat(change.title()).isEqualTo("Label");
        assertThat(change.oldValueSummary()).isEqualTo("Has management identified significant estimates?");
        assertThat(change.newValueSummary()).isEqualTo(
                "Has management identified significant accounting estimates and related estimation uncertainty?");
    }

    @Test
    @DisplayName("reads the section from the JSON pointer and humanizes its display name")
    void derivesSectionFromThePointer() {
        assertThat(summaryOf("ENG-1003").changes())
                .extracting(HumanReadableChange::sectionKey, HumanReadableChange::sectionDisplayName)
                .containsExactly(
                        tuple("planning", "Planning"),
                        tuple("materiality", "Materiality"),
                        tuple("planning", "Planning"),
                        tuple("planning", "Planning"),
                        tuple("materiality", "Materiality"),
                        tuple("completion", "Completion"));
    }

    @Test
    @DisplayName("leaves changes outside the sections scope without a section")
    void leavesChangesOutsideSectionsWithoutSection() {
        HumanReadableChange change = changeAt("ENG-1007", "/metadata/displayName");

        assertThat(change.sectionKey()).isNull();
        assertThat(change.sectionDisplayName()).isNull();
        assertThat(change.title()).isEqualTo("Display Name");
        assertThat(change.changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(change.impactLevel()).isEqualTo(ImpactLevel.MEDIUM);
        assertThat(change.actionRequired()).isTrue();
        assertThat(change.oldValueSummary()).isEqualTo("Canadian Review Engagement");
        assertThat(change.newValueSummary()).isEqualTo("Canadian Review Engagement 2026");
        assertThat(change.description()).isEqualTo("Changed /metadata/displayName from "
                + "\"Canadian Review Engagement\" to \"Canadian Review Engagement 2026\".");
    }

    @Test
    @DisplayName("summarises payloads with a label and renders scalars as plain text")
    void summarisesPayloadsForHumans() {
        HumanReadableChange added = changeAt("ENG-1002", "/sections/completion/checklists/subsequent-events");

        assertThat(added.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(added.title()).isEqualTo("Subsequent events review");
        assertThat(added.oldValueSummary()).isNull();
        assertThat(added.newValueSummary()).isEqualTo("Subsequent events review");
        assertThat(added.description()).isEqualTo("Added \"Subsequent events review\".");

        HumanReadableChange removed = changeAt("ENG-1003", "/sections/planning/procedures/legacy-risk-confirmation");

        assertThat(removed.changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(removed.title()).isEqualTo("Confirm legacy risk classification");
        assertThat(removed.oldValueSummary()).isEqualTo("Confirm legacy risk classification");
        assertThat(removed.newValueSummary()).isNull();
        assertThat(removed.description()).isEqualTo("Removed \"Confirm legacy risk classification\".");

        HumanReadableChange threshold = changeAt("ENG-1002", "/sections/materiality/guidance/thresholdPercent");

        assertThat(threshold.title()).isEqualTo("Threshold Percent");
        assertThat(threshold.oldValueSummary()).isEqualTo("4.5");
        assertThat(threshold.newValueSummary()).isEqualTo("4.0");
        assertThat(threshold.description()).isEqualTo(
                "Changed /sections/materiality/guidance/thresholdPercent from \"4.5\" to \"4.0\".");
    }

    @Test
    @DisplayName("renders a payload without a label as compact JSON")
    void rendersPayloadsWithoutLabelAsJson() {
        RawTemplateDiff synthetic = new RawTemplateDiff("AUDIT-CA", 5, 6, "2026-09-01T10:00:00Z",
                List.of(new RawChange("add", "/sections/planning/questions/8", null, null,
                        payload("id", "Q-PLN-008", "required", true))));
        EngagementTransformationService syntheticService = new EngagementTransformationService(
                List.of(new Template("AUDIT-CA", "Canadian Audit Engagement", 6, List.of())),
                List.of(synthetic),
                FIXED_CLOCK);

        HumanReadableChange change = syntheticService
                .buildPendingUpdateSummary(new Engagement("ENG-0001", "Synthetic engagement", "AUDIT-CA", 5))
                .changes()
                .get(0);

        assertThat(change.id()).isEqualTo("/sections/planning/questions/8");
        assertThat(change.newValueSummary()).isEqualTo("{\"id\":\"Q-PLN-008\",\"required\":true}");
        assertThat(change.oldValueSummary()).isNull();
        assertThat(change.title()).isEqualTo("Questions");
        assertThat(change.sectionKey()).isEqualTo("planning");
    }

    @Test
    @DisplayName("humanizes kebab case section keys and falls back to the closest named segment")
    void humanizesKebabCaseSectionKeys() {
        RawTemplateDiff synthetic = new RawTemplateDiff("AUDIT-CA", 5, 6, "2026-09-01T10:00:00Z",
                List.of(new RawChange("remove", "/sections/going-concern/questions/1", "Legacy answer", null, null)));
        EngagementTransformationService syntheticService = new EngagementTransformationService(
                List.of(new Template("AUDIT-CA", "Canadian Audit Engagement", 6, List.of())),
                List.of(synthetic),
                FIXED_CLOCK);

        HumanReadableChange change = syntheticService
                .buildPendingUpdateSummary(new Engagement("ENG-0002", "Synthetic engagement", "AUDIT-CA", 5))
                .changes()
                .get(0);

        assertThat(change.sectionKey()).isEqualTo("going-concern");
        assertThat(change.sectionDisplayName()).isEqualTo("Going Concern");
        assertThat(change.title()).isEqualTo("Questions");
        assertThat(change.changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(change.oldValueSummary()).isEqualTo("Legacy answer");
        assertThat(change.description()).isEqualTo("Removed /sections/going-concern/questions/1.");
    }

    @Test
    @DisplayName("returns an empty summary for an engagement that is already up to date")
    void returnsEmptySummaryForUpToDateEngagement() {
        PendingUpdateSummaryResponse summary = summaryOf("ENG-1001");

        assertThat(summary.engagementId()).isEqualTo("ENG-1001");
        assertThat(summary.engagementName()).isEqualTo("Northstar Manufacturing 2026");
        assertThat(summary.templateId()).isEqualTo("AUDIT-CA");
        assertThat(summary.templateDisplayName()).isEqualTo("Canadian Audit Engagement");
        assertThat(summary.currentVersion()).isEqualTo(5);
        assertThat(summary.targetVersion()).isEqualTo(5);
        assertThat(summary.status()).isEqualTo(UpdateStatus.UP_TO_DATE);
        assertThat(summary.accumulatedVersions()).isEmpty();
        assertThat(summary.changes()).isEmpty();
        assertThat(summary.totalChangesCount()).isZero();
    }

    @Test
    @DisplayName("stamps the summary with the injected clock")
    void stampsSummaryWithInjectedClock() {
        assertThat(summaryOf("ENG-1002").computedAt()).isEqualTo("2026-09-16T12:00:00Z");
    }

    @Test
    @DisplayName("builds the engagement list without resolving any diff")
    void buildsEngagementListWithoutResolvingDiffs() {
        EngagementTransformationService partial = serviceWithout(diff ->
                diff.templateId().equals("REVIEW-CA") && diff.fromVersion() == 7);
        List<Engagement> reviewEngagements = engagements.stream()
                .filter(engagement -> engagement.templateId().equals("REVIEW-CA"))
                .toList();

        EngagementListResponse response = partial.buildEngagementList(reviewEngagements);

        assertThat(response.totalCount()).isEqualTo(4);
        assertThat(response.pendingUpdatesCount()).isEqualTo(2);
        assertThat(response.engagements()).filteredOn(EngagementItemDto::hasPendingUpdates)
                .extracting(EngagementItemDto::engagementId)
                .containsExactly("ENG-1006", "ENG-1007");
    }

    @Test
    @DisplayName("fails when a link of the chained upgrade path is missing")
    void failsOnIncompleteUpgradePath() {
        EngagementTransformationService partial = serviceWithout(diff ->
                diff.templateId().equals("RISK-CA") && diff.fromVersion() == 11 && diff.toVersion() == 12);
        Engagement behind = engagementOf("ENG-1011");

        assertThat(partial.transform(behind).status()).isEqualTo(UpdateStatus.PENDING_UPDATE);
        assertThatThrownBy(() -> partial.resolveAccumulatedVersions("RISK-CA", 10, 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("10 to 12");
        assertThatThrownBy(() -> partial.buildPendingUpdateSummary(behind))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RISK-CA")
                .hasMessageContaining("11 and 12")
                .hasMessageContaining("10 to 12");
    }

    @Test
    @DisplayName("rejects unsupported diff operations")
    void rejectsUnsupportedOperations() {
        assertThatThrownBy(() -> ChangeType.fromOperation("move"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("move");
        assertThatThrownBy(() -> ChangeType.fromOperation(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("treats a null collection as empty")
    void treatsNullCollectionsAsEmpty() {
        assertThat(service.transformAll(null)).isEmpty();
        assertThat(service.buildEngagementList(null).engagements()).isEmpty();
        assertThat(service.buildEngagementList(null).totalCount()).isZero();
    }
}
