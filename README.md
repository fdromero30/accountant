# Caseware engagement updates

Telling an accountant **which engagements are behind the latest template version** and **what changed
in the template**, without asking them to read a JSON diff.

The repository holds the two halves of that feature:

| Module | Role | Stack |
| --- | --- | --- |
| [`caseware-engagement-transformation/`](caseware-engagement-transformation) | Turns the raw template diffs published by the template pipeline into engagement level statuses and human readable changes | Java 17, Maven, Jackson, JUnit 5 + AssertJ |
| [`caseware-engagement-app/`](caseware-engagement-app) | Renders the engagement list and the read only update summary of one engagement | Angular 20 (standalone, zoneless, signals), `@ngrx/signals`, Sass, Karma |

The library owns the domain and produces the two response payloads; the client owns the presentation
and renders them as they arrive.

```
engagements.json  ─┐
templates.json   ──┼──▶ EngagementTransformationService ──┬──▶ EngagementListResponse         ──┐
diffs/*.json     ──┘                                      └──▶ PendingUpdateSummaryResponse   ──┤
                                                                                                 │
                             EngagementApiService ──▶ EngagementStore ──▶ list screen / modal ◀───┘
```

The wire contract is mirrored field for field, so a response can be typed without any mapping layer:
every interface of
[`core/models/engagement.model.ts`](caseware-engagement-app/src/app/core/models/engagement.model.ts)
matches one Java record of `com.caseware.transformation.model.output`.

The client is not wired to HTTP yet: `EngagementApiService` answers from static fixtures that are a
verbatim copy of the service output, and every method documents the endpoint that will replace it
(see [Scope and known gaps](#scope-and-known-gaps)).

## Repository layout

```
accountant/
├── caseware-engagement-app/                 Angular client
│   ├── src/app/core/                        models, api service and signal store
│   ├── src/app/features/                     engagement list and update summary modal
│   ├── src/app/shared/fixtures/              stubbed payloads of the backend
│   └── src/styles/                           Sass tokens and mixins
├── caseware-engagement-transformation/      Java library
│   ├── src/main/java/com/caseware/           input/output model and service
│   └── src/test/                             tests and JSON fixtures
└── .gitignore
```

Each module carries its own build descriptor (`pom.xml`, `package.json`) and can be built and tested
on its own.

## Requirements

| Module | Needs |
| --- | --- |
| `caseware-engagement-transformation` | JDK 17+ and Maven 3.9+ |
| `caseware-engagement-app` | Node `^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0` and npm (Angular 20 requirement) |

## Getting started

```bash
# Java library: compile and run its tests
cd caseware-engagement-transformation && mvn test

# Angular client: install, serve and open http://localhost:4200
cd caseware-engagement-app && npm install && npm start
```

The client is self contained while the backend is not wired in, so it renders the full list and the
summary modal from its own fixtures without any server running.

## Tests

| Module | Command | Coverage |
| --- | --- | --- |
| `caseware-engagement-transformation` | `mvn test` | 32 tests: `EngagementTransformationServiceTest` (26) and `InputModelValidationTest` (6) |
| `caseware-engagement-app` | `npm test` | 15 specs: `EngagementListComponent` (8) and `UpdateSummaryModalComponent` (7) |

To run the client suite once, without the interactive watcher, use
`npx ng test --watch=false --browsers=ChromeHeadless`.

## Scope and known gaps

* **Display only.** Both halves show what the backend returns: there is no search, no filter and no
  sorting, and applying an update is out of scope, so the modal has nothing to approve or deny.
* **The client does not talk HTTP yet.** `EngagementApiService` answers from the fixtures under
  `shared/fixtures` and each method carries a `TODO(api)` naming the endpoint and the call that have
  to replace it.
* **Only the modal ships styles.** The global sheet exposes the Sass tokens as CSS custom properties,
  so the list screen renders markup whose `.badge`, `.badge-pending`, `.muted` and `.error` classes
  still have no rules.
* **No end-to-end suite.** The `angular.json` of the client defines no `e2e` target, so the feature is
  covered by the unit tests above.

## Java library: `caseware-engagement-transformation/`

Java library that turns the **raw template diffs** published by the template pipeline into
**human readable, engagement level information**: what changed, how disruptive it is and which
engagements have updates waiting.

```
Engagement + Template catalog + RawTemplateDiff  ->  EngagementTransformationService  ->  EngagementListResponse
                                                                                          PendingUpdateSummaryResponse
```

### Module layout

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
└── pom.xml
```

### Input model

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

### Output model

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

### Service behaviour

`EngagementTransformationService` is built once with the template catalog and the published diffs,
and is then reused for any number of engagements.

#### Resolving pending changes

1. If the engagement already runs on `latestVersion` (or ahead of it), the status is `UP_TO_DATE`
   and no changes are reported.
2. If a diff was published **directly** for the `currentVersion -> latestVersion` route, that diff is
   used as-is. This is the preferred path because the publisher already squashed intermediate
   versions (for example `REVIEW-CA 6 -> 8`).
3. Otherwise the consecutive single version diffs (`v -> v+1`) are chained. When a link of the chain
   is missing, the upgrade path cannot be described reliably, so an `IllegalStateException` is thrown
   instead of silently reporting an incomplete list.

Template identifiers are matched case-insensitively.

#### How the changes are presented

* `title` is the label carried by the change payload when there is one (for example
  `Subsequent events review`), otherwise the closest named segment of the JSON pointer, humanized
  (`/sections/planning/questions/3/label` becomes `Label`).
* `oldValueSummary` / `newValueSummary` render the payloads: a `label` is preferred, scalars are
  rendered as plain text and anything else falls back to compact JSON.
* `sectionKey` is read straight from the pointer (`/sections/<key>/...`) and `sectionDisplayName`
  humanizes it (`riskAssessment` becomes `Risk Assessment`, `going-concern` becomes `Going Concern`).
  Changes outside `/sections` (for example `/metadata/displayName`) have no section.
* `actionRequired` is `true` for every impact level except `LOW`.

#### Deriving the impact level

| Raw change | Impact | Rationale |
| --- | --- | --- |
| `remove` | `HIGH` | Answers already captured on the engagement may need remediation |
| `add` | `MEDIUM` | The engagement has to be updated before it can be completed |
| `replace` on a `.../label` path | `LOW` | Purely cosmetic wording change |
| any other `replace` | `MEDIUM` | Settings, thresholds or guidance that affect the work performed |

#### Templates missing from the catalog

An engagement always references a template, so an entry missing from the catalog is a broken input
rather than a state to render: `transform`, `buildEngagementList` and `buildPendingUpdateSummary`
throw an `IllegalArgumentException` naming the engagement and the unknown template. The failure is
deliberately loud, because silently treating such an engagement as up to date would hide real data
problems.

Template identifiers are matched case-insensitively, so `" audit-ca "` resolves to `AUDIT-CA`.

### Usage

```java
EngagementTransformationService service =
        new EngagementTransformationService(templates, diffs);

EngagementListResponse list = service.buildEngagementList(engagements);
PendingUpdateSummaryResponse summary = service.buildPendingUpdateSummary(engagement);
```

A single engagement is summarised by passing the engagement itself; the summary is stamped with the
clock the service was built with (`Clock.systemUTC()` by default, or an explicit clock for tests).

### Build and test

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

### Fixtures

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

The client replays exactly this fixture set, so both halves are documented against the same dataset.

## Angular client: `caseware-engagement-app/`

Angular 20 client for the same feature: the list of engagements with the template version they run on,
plus the read only summary of the changes waiting for one engagement.

```
screens  ──▶  EngagementStore  ──▶  EngagementApiService  ──▶  EngagementListResponse
                                                                PendingUpdateSummaryResponse
```

### Screens

| Screen | Component | Content |
| --- | --- | --- |
| Engagement list (route `''`, `**` redirects here) | `EngagementListComponent` | Totals of the response plus one row per engagement: Engagement, Template, Version (`current -> latest`), Status and the `Review update` action |
| Update summary | `UpdateSummaryModalComponent` | Opened from a row: the upgrade route, the applied diffs, the change counters and the changes grouped by template section |

Rows of engagements that are already up to date keep the review action disabled, and `openSummary`
ignores them outright. The modal closes on the `Close` button, the icon button, a click on the
backdrop or `Escape`; it has no approve/deny action because applying an update is out of scope.

### Module layout

```
src/
├── app/
│   ├── core/
│   │   ├── models/        engagement.model.ts: wire contract, labels and grouping helpers
│   │   ├── services/      engagement-api.service.ts: the stubbed data access
│   │   └── store/         engagement.store.ts: the signalStore of the screens
│   ├── features/
│   │   ├── engagement-list/
│   │   └── update-summary-modal/
│   ├── shared/fixtures/   mock-engagements.ts: payloads copied from the service output
│   ├── app.config.ts      zoneless change detection, router and HttpClient
│   └── app.routes.ts      the list route
├── styles.scss            global entry point
└── styles/                Sass tokens and mixins
```

### Data access

`EngagementApiService` is the only place that will talk to the backend. It answers from the fixtures
until the endpoints exist, and each method names the request that has to replace it:

| Method | Endpoint it will call |
| --- | --- |
| `getEngagements()` | `GET {baseUrl}/engagements` |
| `getPendingUpdateSummary(engagementId)` | `GET {baseUrl}/engagements/{engagementId}/pending-update-summary` |

The simulated round trip is driven by the `API_LATENCY_MS` token: `250` ms in the app, `0` in the
specs, so the tests stay synchronous.

### State

`EngagementStore` is a root `signalStore` and the single source of truth of both screens, so they
always share the same instance.

| Kind | Members |
| --- | --- |
| State | `engagements`, `totalCount`, `pendingUpdatesCount`, `selectedEngagementId`, `summary`, `listLoading`, `summaryLoading`, `listError`, `summaryError` |
| Computed | `selectedEngagement`, `summaryChanges`, `isEmpty`, `isSummaryOpen`, `summaryGroups`, `summaryImpactCounts`, `actionRequiredCount` |
| Methods | `loadEngagements()`, `openSummary(engagementId)`, `closeSummary()` |

The components never touch the service: they call store methods and read the store signals, and the
store is where every request is triggered and where the response is patched in. Reopening the modal of
the engagement that is already loaded is served from the state, so the endpoint is not called twice.

### Styling with Sass

The app is styled with [Sass](https://sass-lang.com/) (SCSS syntax, compiled by the Angular CLI):

* `src/styles.scss` is the global entry point declared in the `styles` array of `angular.json`. It
  only holds global rules and exposes the design tokens as CSS custom properties.
* `src/styles/_tokens.scss` keeps the variables (palette, spacing, font sizes, radii) and
  `src/styles/_mixins.scss` the shared mixins. Both are partials, so they never compile on their own.
* Components pull them in with `@use 'tokens' as *;` and `@use 'mixins' as *;`. The bare specifier
  resolves because `stylePreprocessorOptions.includePaths` points at `src/styles`.
* Component styles live next to their component as `*.component.scss` and stay encapsulated by the
  Angular view encapsulation, so a component only needs `@use` when it uses tokens or mixins.
* The `@schematics/angular:component` schematic of `angular.json` is set to `scss`, so
  `ng generate component` follows the same convention.

### Scripts

| Command | Does |
| --- | --- |
| `npm start` | `ng serve` on `http://localhost:4200` |
| `npm run build` | production build into `dist/` |
| `npm run watch` | development build in watch mode |
| `npm test` | Karma in watch mode |
| `npx ng test --watch=false --browsers=ChromeHeadless` | the suite once, without the watcher |

### Tests

The 15 specs render the real components against the real store and only replace the API service when
the state under test needs a different response:

* `EngagementListComponent` (8): one row per engagement with its template version, the list loaded
  through the store, the status markers, the empty state, the review action enabled only when there is
  something to review, opening the summary of a pending engagement, ignoring one that is up to date
  and closing the modal.
* `UpdateSummaryModalComponent` (7): nothing rendered before a selection, the upgrade route and the
  accumulated diffs, the grouping by section (including the `General` heading of the changes without a
  section), the counters per impact level and the ones requiring action, loading the summary only once
  and closing from the button and from the backdrop.

### Conventions

* Standalone components with `ChangeDetectionStrategy.OnPush`, zoneless change detection
  (`provideZonelessChangeDetection`) and signals for all state.
* Domain types, display labels and pure helpers live in `core/models`, so templates only format.
* Strict TypeScript plus the `strictTemplates`, `strictInjectionParameters` and
  `strictInputAccessModifiers` flags of `tsconfig.json`: a template that reads an unknown member fails
  the build.
* Templates use the built in control flow (`@if`, `@for`) and expose `data-testid` hooks for the specs.
