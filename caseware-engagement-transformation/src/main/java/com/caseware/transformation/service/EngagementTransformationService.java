package com.caseware.transformation.service;

import com.caseware.transformation.model.input.Engagement;
import com.caseware.transformation.model.input.RawChange;
import com.caseware.transformation.model.input.RawTemplateDiff;
import com.caseware.transformation.model.input.Template;
import com.caseware.transformation.model.output.ChangeType;
import com.caseware.transformation.model.output.EngagementItemDto;
import com.caseware.transformation.model.output.EngagementListResponse;
import com.caseware.transformation.model.output.HumanReadableChange;
import com.caseware.transformation.model.output.ImpactLevel;
import com.caseware.transformation.model.output.PendingUpdateSummaryResponse;
import com.caseware.transformation.model.output.UpdateStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Turns raw template diffs into the engagement level information consumed by the UI.
 *
 * <p>The service is stateless between calls: it is built once with the current template catalog and
 * the set of published diffs, and can then be reused for any number of engagements. Every engagement
 * is expected to reference a template that exists in the catalog; engagements are never created
 * without one.</p>
 *
 * <h2>How pending changes are resolved</h2>
 * <ol>
 *   <li>If the engagement already runs on the latest published version, the status is
 *       {@link UpdateStatus#UP_TO_DATE} and no changes are reported.</li>
 *   <li>If a diff was published directly for the {@code currentVersion -> latestVersion} route it is
 *       used as-is. This is the preferred path because the publisher has already squashed any
 *       intermediate versions.</li>
 *   <li>Otherwise the consecutive single version diffs ({@code v -> v+1}) are chained. When one of
 *       those links is missing, the upgrade path cannot be described reliably and an
 *       {@link IllegalStateException} is thrown rather than silently reporting an incomplete list.</li>
 * </ol>
 *
 * <h2>How the upgrade is described</h2>
 * <p>{@code accumulatedVersions} only carries the <em>target</em> version of every applied diff: an
 * engagement going from {@code 3} to {@code 5} through the consecutive diffs {@code 3 -> 4} and
 * {@code 4 -> 5} yields {@code [4, 5]}. Change identifiers are deterministic: they are derived from
 * the JSON pointer of the affected node, so the same catalog plus the same diffs always produce the
 * same response.</p>
 *
 * <h2>How impact is derived</h2>
 * <ul>
 *   <li>{@link ChangeType#REMOVED} is {@link ImpactLevel#HIGH}: already captured answers may need
 *       remediation.</li>
 *   <li>{@link ChangeType#ADDED} is {@link ImpactLevel#MEDIUM}: the engagement must be updated
 *       before it can be completed.</li>
 *   <li>{@link ChangeType#MODIFIED} is {@link ImpactLevel#LOW} when only a {@code label} node
 *       changed (purely cosmetic) and {@link ImpactLevel#MEDIUM} otherwise.</li>
 * </ul>
 *
 * <h2>How sections are named</h2>
 * <p>The catalog contract carries no section metadata, so {@code sectionKey} is read straight from
 * the JSON pointer ({@code /sections/<key>/...}) and its display name is derived from the key
 * ({@code riskAssessment} becomes {@code Risk Assessment}). Should the publisher start exposing real
 * section names, {@link #sectionDisplayNameOf(String)} is the only place that has to change.</p>
 */
public class EngagementTransformationService {

    private static final String SECTIONS_SEGMENT = "/sections/";
    private static final String LABEL_SEGMENT = "/label";
    private static final String LABEL_KEY = "label";

    private final Map<String, Template> templatesById;
    private final Map<String, RawTemplateDiff> diffsByRoute;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Creates the service using the system UTC clock.
     *
     * @param templates the template catalog
     * @param diffs     the published template diffs
     */
    public EngagementTransformationService(Collection<Template> templates, Collection<RawTemplateDiff> diffs) {
        this(templates, diffs, Clock.systemUTC());
    }

    /**
     * Creates the service with an explicit clock, which keeps the {@code computedAt} timestamp of the
     * pending update summary deterministic in tests.
     *
     * @param templates the template catalog
     * @param diffs     the published template diffs
     * @param clock     clock used to stamp the generated responses
     * @throws IllegalArgumentException when two diffs share the same template/from/to route
     */
    public EngagementTransformationService(Collection<Template> templates,
                                           Collection<RawTemplateDiff> diffs,
                                           Clock clock) {
        this.objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.templatesById = indexTemplates(templates);
        this.diffsByRoute = indexDiffs(diffs);
    }

    /**
     * Builds the payload backing the engagement list screen.
     *
     * @param engagements engagements to transform, in display order
     * @return the engagement list response; never {@code null}
     */
    public EngagementListResponse buildEngagementList(Collection<Engagement> engagements) {
        List<EngagementItemDto> items = transformAll(engagements);
        int pendingUpdatesCount = (int) items.stream()
                .filter(EngagementItemDto::hasPendingUpdates)
                .count();
        return new EngagementListResponse(items.size(), pendingUpdatesCount, items);
    }

    /**
     * Builds the detailed view of the updates waiting for a single engagement, which is what the UI
     * shows once the user opens an engagement that is behind its template.
     *
     * @param engagement the engagement to describe
     * @return the pending update summary; never {@code null}
     * @throws IllegalArgumentException when the engagement references a template missing from the catalog
     * @throws IllegalStateException    when the upgrade path cannot be fully described
     */
    public PendingUpdateSummaryResponse buildPendingUpdateSummary(Engagement engagement) {
        EngagementItemDto item = transform(engagement);
        List<HumanReadableChange> changes = item.hasPendingUpdates()
                ? resolveChanges(item.templateId(), item.currentVersion(), item.latestVersion())
                : List.of();
        List<Integer> accumulatedVersions = item.hasPendingUpdates()
                ? resolveAccumulatedVersions(item.templateId(), item.currentVersion(), item.latestVersion())
                : List.of();

        return new PendingUpdateSummaryResponse(
                item.engagementId(),
                item.name(),
                item.templateId(),
                item.templateDisplayName(),
                item.currentVersion(),
                item.latestVersion(),
                accumulatedVersions,
                item.status(),
                Instant.now(clock).toString(),
                changes.size(),
                changes);
    }

    /**
     * Transforms every given engagement, preserving the iteration order of the input.
     *
     * @param engagements engagements to transform
     * @return the transformed engagements; never {@code null}
     */
    public List<EngagementItemDto> transformAll(Collection<Engagement> engagements) {
        if (engagements == null) {
            return List.of();
        }
        return engagements.stream().map(this::transform).toList();
    }

    /**
     * Transforms a single engagement: resolves its template, compares the version it lives on with
     * the latest published one and derives the status the list screen renders.
     *
     * @param engagement the engagement to transform
     * @return the transformed engagement; never {@code null}
     * @throws IllegalArgumentException when the engagement references a template missing from the catalog
     */
    public EngagementItemDto transform(Engagement engagement) {
        Objects.requireNonNull(engagement, "engagement must not be null");
        Template template = templatesById.get(normalizeId(engagement.templateId()));
        if (template == null) {
            throw new IllegalArgumentException("Unknown template " + engagement.templateId()
                    + " referenced by engagement " + engagement.engagementId());
        }

        int currentVersion = engagement.templateVersion();
        int latestVersion = template.latestVersion();
        int versionsBehind = Math.max(0, latestVersion - currentVersion);
        UpdateStatus status = versionsBehind == 0 ? UpdateStatus.UP_TO_DATE : UpdateStatus.PENDING_UPDATE;

        return new EngagementItemDto(
                engagement.engagementId(),
                engagement.name(),
                template.templateId(),
                template.displayName(),
                currentVersion,
                latestVersion,
                versionsBehind,
                status);
    }

    /**
     * Resolves the changes an engagement would see when upgrading from {@code fromVersion} to
     * {@code toVersion}.
     *
     * @param templateId  template the engagement is based on
     * @param fromVersion version the engagement currently lives on
     * @param toVersion   version the engagement would be upgraded to
     * @return the human readable changes in diff order; never {@code null}
     * @throws IllegalStateException when the upgrade path cannot be fully described
     */
    public List<HumanReadableChange> resolveChanges(String templateId, int fromVersion, int toVersion) {
        Set<String> usedIds = new LinkedHashSet<>();
        List<HumanReadableChange> changes = new ArrayList<>();
        for (RawTemplateDiff diff : resolveDiffs(templateId, fromVersion, toVersion)) {
            for (RawChange change : diff.changes()) {
                changes.add(toHumanReadableChange(change, uniqueId(change.path(), usedIds)));
            }
        }
        return List.copyOf(changes);
    }

    /**
     * Resolves the versions whose diffs have to be accumulated to describe the upgrade.
     *
     * <p>Only the target version of every applied diff is reported: an engagement going from 3 to 5
     * through the consecutive diffs {@code 3 -> 4} and {@code 4 -> 5} yields {@code [4, 5]}, while an
     * engagement going from 6 to 8 through a directly published {@code 6 -> 8} diff yields
     * {@code [8]}.</p>
     *
     * @param templateId  template the engagement is based on
     * @param fromVersion version the engagement currently lives on
     * @param toVersion   version the engagement would be upgraded to
     * @return the target versions in upgrade order; empty when there is nothing to apply
     */
    public List<Integer> resolveAccumulatedVersions(String templateId, int fromVersion, int toVersion) {
        return resolveDiffs(templateId, fromVersion, toVersion).stream()
                .map(RawTemplateDiff::toVersion)
                .toList();
    }

    /**
     * Picks the diffs that describe the upgrade. A diff published directly for the
     * {@code fromVersion -> toVersion} route wins over chained consecutive diffs because the
     * publisher has already squashed the intermediate versions.
     *
     * @param templateId  template the engagement is based on
     * @param fromVersion version the engagement currently lives on
     * @param toVersion   version the engagement would be upgraded to
     * @return the diffs to apply, in upgrade order; empty when there is nothing to apply
     * @throws IllegalStateException when a consecutive diff of the upgrade path is missing
     */
    private List<RawTemplateDiff> resolveDiffs(String templateId, int fromVersion, int toVersion) {
        if (toVersion <= fromVersion) {
            return List.of();
        }

        RawTemplateDiff direct = diffsByRoute.get(routeKey(templateId, fromVersion, toVersion));
        if (direct != null) {
            return List.of(direct);
        }

        List<RawTemplateDiff> chained = new ArrayList<>();
        for (int version = fromVersion; version < toVersion; version++) {
            RawTemplateDiff step = diffsByRoute.get(routeKey(templateId, version, version + 1));
            if (step == null) {
                throw new IllegalStateException("Missing diff for template " + templateId
                        + " between versions " + version + " and " + (version + 1)
                        + ": cannot describe the upgrade from " + fromVersion + " to " + toVersion);
            }
            chained.add(step);
        }
        return List.copyOf(chained);
    }

    /**
     * Renders one raw change for the UI.
     *
     * @param change raw change coming from the diff
     * @param id     deterministic identifier to assign to the change
     * @return the human readable change; never {@code null}
     */
    private HumanReadableChange toHumanReadableChange(RawChange change, String id) {
        ChangeType changeType = ChangeType.fromOperation(change.op());
        String path = change.path();
        String label = change.introducedValue()
                .or(change::removedValue)
                .map(EngagementTransformationService::labelOf)
                .orElse(null);
        String sectionKey = sectionKeyOf(path);
        String oldValueSummary = change.removedValue().map(this::renderValue).orElse(null);
        String newValueSummary = change.introducedValue().map(this::renderValue).orElse(null);
        ImpactLevel impactLevel = impactOf(changeType, path);

        return new HumanReadableChange(
                id,
                sectionKey,
                sectionDisplayNameOf(sectionKey),
                changeType,
                titleOf(label, path),
                describe(changeType, path, label, oldValueSummary, newValueSummary),
                impactLevel,
                actionRequiredOf(impactLevel),
                oldValueSummary,
                newValueSummary);
    }

    /**
     * Builds an identifier that is unique inside a single response while staying readable and
     * deterministic: the JSON pointer of the node, suffixed with {@code #2}, {@code #3} and so on
     * when the same node is touched more than once within one upgrade.
     *
     * @param path    JSON pointer of the changed node
     * @param usedIds identifiers already handed out while resolving the same upgrade
     * @return the identifier to assign to the change
     */
    private static String uniqueId(String path, Set<String> usedIds) {
        String base = path == null || path.isBlank() ? "/" : path;
        String candidate = base;
        int occurrence = 2;
        while (!usedIds.add(candidate)) {
            candidate = base + "#" + occurrence++;
        }
        return candidate;
    }

    /**
     * @param changeType normalized change type
     * @param path       JSON pointer of the changed node
     * @return how disruptive the change is for the engagement
     */
    private static ImpactLevel impactOf(ChangeType changeType, String path) {
        return switch (changeType) {
            case REMOVED -> ImpactLevel.HIGH;
            case ADDED -> ImpactLevel.MEDIUM;
            case MODIFIED -> isLabelChange(path) ? ImpactLevel.LOW : ImpactLevel.MEDIUM;
        };
    }

    /**
     * @param impactLevel impact of a change
     * @return {@code true} when the accountant has to act on it, i.e. anything but {@link ImpactLevel#LOW}
     */
    private static boolean actionRequiredOf(ImpactLevel impactLevel) {
        return impactLevel != ImpactLevel.LOW;
    }

    /**
     * @param path JSON pointer of the changed node
     * @return {@code true} when only a human readable label changed, which is purely cosmetic
     */
    private static boolean isLabelChange(String path) {
        return path != null && path.endsWith(LABEL_SEGMENT);
    }

    /**
     * Builds the sentence shown under the change title.
     *
     * @param changeType      normalized change type
     * @param path            JSON pointer of the changed node
     * @param label           human readable label of the node, when the payload carries one
     * @param oldValueSummary readable summary of the previous value, when applicable
     * @param newValueSummary readable summary of the new value, when applicable
     * @return the plain language description; never {@code null}
     */
    private static String describe(ChangeType changeType,
                                   String path,
                                   String label,
                                   String oldValueSummary,
                                   String newValueSummary) {
        String subject = label != null ? '"' + label + '"' : String.valueOf(path);
        return switch (changeType) {
            case REMOVED -> "Removed " + subject + ".";
            case ADDED -> "Added " + subject + ".";
            case MODIFIED -> {
                if (oldValueSummary != null && newValueSummary != null) {
                    yield "Changed " + subject + " from \"" + oldValueSummary + "\" to \"" + newValueSummary + "\".";
                }
                yield "Updated " + subject + ".";
            }
        };
    }

    /**
     * Renders a raw JSON value as text for the summary fields.
     *
     * <p>Scalars are rendered as plain text, maps exposing a non blank {@code label} are rendered as
     * that label (the most meaningful part of a template node) and everything else falls back to a
     * compact JSON rendering.</p>
     *
     * @param value raw value coming from the diff
     * @return the readable summary, or {@code null} when there is no value to summarise
     */
    private String renderValue(Object value) {
        if (value == null) {
            return null;
        }
        String label = labelOf(value);
        if (label != null) {
            return label;
        }
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot render value " + value + " as text", e);
        }
    }

    /**
     * @param value raw value coming from the diff
     * @return the non blank {@code label} of a map payload, or {@code null} when there is none
     */
    private static String labelOf(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object label = map.get(LABEL_KEY);
            if (label instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    /**
     * Reads the section a change belongs to from its JSON pointer.
     *
     * @param path JSON pointer of the changed node
     * @return the section key, or {@code null} when the change is not scoped to a section
     */
    private static String sectionKeyOf(String path) {
        if (path == null) {
            return null;
        }
        int start = path.indexOf(SECTIONS_SEGMENT);
        if (start < 0) {
            return null;
        }
        String remainder = path.substring(start + SECTIONS_SEGMENT.length());
        int end = remainder.indexOf('/');
        String key = end < 0 ? remainder : remainder.substring(0, end);
        return key.isBlank() ? null : key;
    }

    /**
     * Derives the display name of a section from its key.
     *
     * @param sectionKey section key, e.g. {@code riskAssessment}
     * @return the human readable section name, or {@code null} when there is no section
     */
    private static String sectionDisplayNameOf(String sectionKey) {
        return sectionKey == null ? null : humanize(sectionKey);
    }

    /**
     * Turns a template key into a display name: {@code riskAssessment} becomes {@code Risk
     * Assessment} and {@code going-concern} becomes {@code Going Concern}. Keys without any lower
     * case character (acronyms, numerals) are left untouched.
     *
     * @param raw raw key
     * @return the human readable name; never {@code null}
     */
    private static String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        if (raw.equals(raw.toUpperCase(Locale.ROOT))) {
            return raw;
        }
        StringBuilder words = new StringBuilder();
        char previous = 0;
        for (char current : raw.toCharArray()) {
            if (isSeparator(current)) {
                if (words.length() > 0 && !endsWithBlank(words)) {
                    words.append(' ');
                }
                previous = current;
                continue;
            }
            boolean startsWord = words.length() == 0
                    || endsWithBlank(words)
                    || (Character.isUpperCase(current)
                        && (Character.isLowerCase(previous) || Character.isDigit(previous)));
            if (startsWord) {
                if (endsWithBlank(words)) {
                    words.setLength(words.length() - 1);
                }
                if (words.length() > 0) {
                    words.append(' ');
                }
                words.append(Character.toUpperCase(current));
            } else {
                words.append(Character.toLowerCase(current));
            }
            previous = current;
        }
        return words.toString();
    }

    private static boolean isSeparator(char current) {
        return current == '-' || current == '_' || current == '.' || Character.isWhitespace(current);
    }

    private static boolean endsWithBlank(StringBuilder text) {
        return text.length() > 0 && text.charAt(text.length() - 1) == ' ';
    }

    /**
     * Builds the headline of a change.
     *
     * @param label human readable label carried by the payload, when there is one
     * @param path  JSON pointer of the changed node
     * @return the headline; never {@code null}
     */
    private static String titleOf(String label, String path) {
        if (label != null) {
            return label;
        }
        String segment = lastDisplaySegment(path);
        return segment == null ? "Template change" : humanize(segment);
    }

    /**
     * @param path JSON pointer of a node
     * @return its most meaningful segment, i.e. the last one that is neither blank nor a bare index,
     *         or {@code null} when the pointer has none
     */
    private static String lastDisplaySegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] segments = path.split("/");
        for (int index = segments.length - 1; index >= 0; index--) {
            String segment = segments[index];
            if (!segment.isBlank() && !segment.chars().allMatch(Character::isDigit)) {
                return segment;
            }
        }
        return null;
    }

    /**
     * @param templateId template identifier as it appears in the input
     * @return the lookup key used by the caches
     */
    private static String normalizeId(String templateId) {
        return templateId == null ? "" : templateId.trim().toLowerCase(Locale.ROOT);
    }

    private static String routeKey(String templateId, int fromVersion, int toVersion) {
        return normalizeId(templateId) + '|' + fromVersion + "->" + toVersion;
    }

    private static Map<String, Template> indexTemplates(Collection<Template> templates) {
        Map<String, Template> index = new LinkedHashMap<>();
        if (templates == null) {
            return Collections.emptyMap();
        }
        for (Template template : templates) {
            index.put(normalizeId(template.templateId()), template);
        }
        return Collections.unmodifiableMap(index);
    }

    private static Map<String, RawTemplateDiff> indexDiffs(Collection<RawTemplateDiff> diffs) {
        Map<String, RawTemplateDiff> index = new LinkedHashMap<>();
        if (diffs == null) {
            return Collections.emptyMap();
        }
        for (RawTemplateDiff diff : diffs) {
            String key = routeKey(diff.templateId(), diff.fromVersion(), diff.toVersion());
            RawTemplateDiff previous = index.put(key, diff);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate diff for template " + diff.templateId()
                        + " between versions " + diff.fromVersion() + " and " + diff.toVersion());
            }
        }
        return Collections.unmodifiableMap(index);
    }
}
