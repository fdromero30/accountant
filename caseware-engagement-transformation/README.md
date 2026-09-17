# Caseware Engagement Transformation

Java library that turns the **raw template diffs** published by the template pipeline into
**human readable, engagement level information**: what changed, how disruptive it is and which
engagements have updates waiting.

```
Engagement + Template catalog + RawTemplateDiff  ->  EngagementTransformationService  ->  EngagementListResponse
                                                                                          PendingUpdateSummaryResponse
```

## Project layout

```
caseware-engagement-transformation/
├── src/
│   ├── main/
│   │   ├── java/com/caseware/transformation/
│   │   │   ├── model/input/     Engagement, Template, TemplateVersionInfo, RawTemplateDiff, RawChange
│   │   │   │                    (plus the package private InputGuard validating the contract)
│   │   │   ├── model/output/    ChangeType, ImpactLevel, UpdateStatus, HumanReadableChange,
│   │   │   │                    EngagementItemDto, EngagementListResponse, PendingUpdateSummaryResponse
│   │   │   └── service/         EngagementTransformationService
│   │   └── resources/
│   └── test/
│       ├── java/com/caseware/transformation/
│       │   ├── model/input/InputModelValidationTest.java
│       │   └── service/EngagementTransformationServiceTest.java
│       └── resources/fixtures/
│           ├── engagements.json
│           ├── templates.json
│           ├── template-fragment-audit-ca-v5.json
│           └── diffs/template-diff-*.json
├── .gitignore
├── pom.xml
└── README.md
```

## Input model

| Type | Source | Notes |
| --- | --- | --- |
| `Engagement` | `engagements.json` | engagement id, name, mandatory template id and the template version it is pinned to (`>= 1`) |
| `Template` | `templates.json` | catalog entry with `latestVersion` (`>= 1`) and the published `TemplateVersionInfo` list |
| `TemplateVersionInfo` | `templates.json` | version number (`>= 1`) plus `publishedAt` |
| `RawTemplateDiff` | `diffs/template-diff-*.json` | `templateId`, `fromVersion`, `toVersion`, `generatedAt`, `changes` |
| `RawChange` | inside a diff | `op` (`add`/`replace`/`remove`), `path` (JSON pointer) and the `value`/`oldValue`/`newValue` payloads |

All input fields are mandatory and template versions start at `1`, so no "before the first version"
sentinel has to be handled. The input records enforce that contract in their canonical constructors:

* an `Engagement` cannot be built without a `templateId`, which makes "an engagement always has a
  template" a property of the model rather than a convention;
* blank identifiers, names, timestamps and change paths are rejected with an
  `IllegalArgumentException` naming the offending field;
* version numbers below `1` (engagement pinned version, template `latestVersion`, published
  `TemplateVersionInfo.version`, diff `fromVersion`/`toVersion`) are rejected the same way.

Because Jackson builds the records through those constructors, a payload that violates the contract
fails deserialization instead of producing a half populated object. All input records ignore unknown
JSON properties, so the publisher can evolve the payload without breaking this library.

## Output model

| Type | Meaning |
| --- | --- |
| `ChangeType` | `ADDED`, `MODIFIED`, `REMOVED`, mapped from the raw `op` |
| `ImpactLevel` | `LOW`, `MEDIUM`, `HIGH`, ordered from least to most disruptive |
| `UpdateStatus` | `UP_TO_DATE` when the engagement runs on `latestVersion`, `PENDING_UPDATE` otherwise |
| `HumanReadableChange` | deterministic `id`, section, change type, title, plain language description, impact, `actionRequired` and rendered old/new values |
| `EngagementItemDto` | one row of the engagement list: engagement, template, current/latest version, `versionsBehind` and status |
| `EngagementListResponse` | `totalCount`, `pendingUpdatesCount` and the list of `EngagementItemDto` |
| `PendingUpdateSummaryResponse` | one engagement: `targetVersion`, the `accumulatedVersions`, `computedAt` and the human readable changes |

`id` is the JSON pointer of the affected node, suffixed with `#2`, `#3` and so on when the same node
is touched more than once by the upgrade. That keeps the identifier **deterministic** (same input,
same output) while staying unique inside a response.

`accumulatedVersions` contains **only the target version of every applied diff**: an engagement
upgraded `3 -> 5` through the consecutive diffs `3 -> 4` and `4 -> 5` yields `[4, 5]`, while an
engagement upgraded `6 -> 8` through a directly published `6 -> 8` diff yields `[8]`.

## Service behaviour

`EngagementTransformationService` is built once with the template catalog and the published diffs,
and is then reused for any number of engagements.

### Resolving pending changes

1. If the engagement already runs on `latestVersion` (or ahead of it), the status is `UP_TO_DATE`
   and no changes are reported.
2. If a diff was published **directly** for the `currentVersion -> latestVersion` route, that diff is
   used as-is. This is the preferred path because the publisher already squashed intermediate
   versions (for example `REVIEW-CA 6 -> 8`).
3. Otherwise the consecutive single version diffs (`v -> v+1`) are chained. When a link of the chain
   is missing, the upgrade path cannot be described reliably, so an `IllegalStateException` is thrown
   instead of silently reporting an incomplete list.

Template identifiers are matched case-insensitively.

### How the changes are presented

* `title` is the label carried by the change payload when there is one (for example
  `Subsequent events review`), otherwise the closest named segment of the JSON pointer, humanized
  (`/sections/planning/questions/3/label` becomes `Label`).
* `oldValueSummary` / `newValueSummary` render the payloads: a `label` is preferred, scalars are
  rendered as plain text and anything else falls back to compact JSON.
* `sectionKey` is read straight from the pointer (`/sections/<key>/...`) and `sectionDisplayName`
  humanizes it (`riskAssessment` becomes `Risk Assessment`, `going-concern` becomes `Going Concern`).
  Changes outside `/sections` (for example `/metadata/displayName`) have no section.
* `actionRequired` is `true` for every impact level except `LOW`.

### Deriving the impact level

| Raw change | Impact | Rationale |
| --- | --- | --- |
| `remove` | `HIGH` | Answers already captured on the engagement may need remediation |
| `add` | `MEDIUM` | The engagement has to be updated before it can be completed |
| `replace` on a `.../label` path | `LOW` | Purely cosmetic wording change |
| any other `replace` | `MEDIUM` | Settings, thresholds or guidance that affect the work performed |

### Templates missing from the catalog

An engagement always references a template, so an entry missing from the catalog is a broken input
rather than a state to render: `transform`, `buildEngagementList` and `buildPendingUpdateSummary`
throw an `IllegalArgumentException` naming the engagement and the unknown template. The failure is
deliberately loud, because silently treating such an engagement as up to date would hide real data
problems.

Template identifiers are matched case-insensitively, so `" audit-ca "` resolves to `AUDIT-CA`.

## Usage

```java
EngagementTransformationService service =
        new EngagementTransformationService(templates, diffs);

EngagementListResponse list = service.buildEngagementList(engagements);
PendingUpdateSummaryResponse summary = service.buildPendingUpdateSummary(engagement);
```

A single engagement is summarised by passing the engagement itself; the summary is stamped with the
clock the service was built with (`Clock.systemUTC()` by default, or an explicit clock for tests).

## Build and test

Requires JDK 17+ and Maven 3.9+.

```bash
mvn test        # compiles and runs both test classes
mvn verify      # full build
```

`EngagementTransformationServiceTest` loads the JSON fixtures from the test classpath and asserts the
statuses, the composed changes, the deterministic identifiers, the impact classification, the
rendered descriptions, the section derivation and the failure modes (duplicate diffs, incomplete
upgrade paths and engagements without a catalog template).

`InputModelValidationTest` covers the input contract itself: an engagement without a template,
missing fields and versions below `1` are rejected.

## Fixtures

The fixtures describe three Canadian templates and twelve engagements:

| Template | Published versions | Engagements |
| --- | --- | --- |
| `AUDIT-CA` | 3, 4, 5 | `ENG-1001`..`ENG-1004` |
| `REVIEW-CA` | 6, 7, 8 | `ENG-1005`..`ENG-1008` |
| `RISK-CA` | 10, 11, 12 | `ENG-1009`..`ENG-1012` |

Across the fixtures, 6 engagements are up to date and 6 have pending updates totalling 26 changes
(3 `HIGH`, 20 `MEDIUM`, 3 `LOW`). `REVIEW-CA 6 -> 8` is the only route with both a direct diff and a
chain of consecutive diffs, and is used to cover the "prefer the direct diff" branch; `AUDIT-CA 3 -> 5`
and `RISK-CA 10 -> 12` cover the chained path, including the `#2` suffix of a JSON pointer touched by
two consecutive diffs.

`template-fragment-audit-ca-v5.json` is a partial snapshot of `AUDIT-CA` version 5 kept as reference
material for the template shape; it is not consumed by the test.
