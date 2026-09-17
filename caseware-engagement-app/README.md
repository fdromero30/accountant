# Caseware engagement updates · Angular client

Angular 20 client of the engagement update feature: the list of engagements with the template version
they run on, plus the read only summary of the changes waiting for one engagement.

The backend half lives in
[`../caseware-engagement-transformation`](../caseware-engagement-transformation); the repository
overview and the full domain documentation are in the [root README](../README.md).

## Requirements

Node `^20.19.0 || ^22.12.0 || >=24.0.0` and npm (the Angular 20 engine requirement).

## Getting started

```bash
npm install
npm start          # ng serve on http://localhost:4200
```

The app is self contained while the backend is not wired in: it renders the whole feature from the
fixtures under `src/app/shared/fixtures`, so no server has to be running.

## Scripts

| Command | Does |
| --- | --- |
| `npm start` | `ng serve` on `http://localhost:4200` |
| `npm run build` | production build into `dist/` |
| `npm run watch` | development build in watch mode |
| `npm test` | Karma in watch mode |
| `npx ng test --watch=false --browsers=ChromeHeadless` | the suite once, without the watcher |

There is no `e2e` target configured, so the feature is covered by the unit specs.

## Structure

```
src/
├── app/
│   ├── core/
│   │   ├── models/        engagement.model.ts: wire contract, labels and grouping helpers
│   │   ├── services/      engagement-api.service.ts: the stubbed data access
│   │   └── store/         engagement.store.ts: the signalStore of the screens
│   ├── features/
│   │   ├── engagement-list/      the list screen, the entry point of the feature
│   │   └── update-summary-modal/ the read only summary of one engagement
│   ├── shared/fixtures/   mock-engagements.ts: payloads copied from the service output
│   ├── app.config.ts      zoneless change detection, router and HttpClient
│   └── app.routes.ts      the list route
├── styles.scss            global entry point
└── styles/                Sass tokens and mixins
```

## Screens

| Screen | Component | Content |
| --- | --- | --- |
| Engagement list (route `''`, `**` redirects here) | `EngagementListComponent` | Totals of the response plus one row per engagement: Engagement, Template, Version (`current -> latest`), Status and the `Review update` action |
| Update summary | `UpdateSummaryModalComponent` | Opened from a row: the upgrade route, the applied diffs, the change counters and the changes grouped by template section |

Rows of engagements that are already up to date keep the review action disabled, and `openSummary`
ignores them outright. The modal closes on the `Close` button, the icon button, a click on the backdrop
or `Escape`; it has no approve/deny action because applying an update is out of scope. Both screens are
display only: there is no search, filter or sorting.

## Data access

`EngagementApiService` is the only place that will talk to the backend. It answers from the fixtures
until the endpoints exist, and each method names the request that has to replace it:

| Method | Endpoint it will call |
| --- | --- |
| `getEngagements()` | `GET {baseUrl}/engagements` |
| `getPendingUpdateSummary(engagementId)` | `GET {baseUrl}/engagements/{engagementId}/pending-update-summary` |

The simulated round trip is driven by the `API_LATENCY_MS` token: `250` ms in the app, `0` in the
specs, so the tests stay synchronous.

## State

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

- `src/styles.scss` is the global entry point declared in the `styles` array of `angular.json`. It only holds global rules and exposes the design tokens as CSS custom properties.
- `src/styles/_tokens.scss` keeps the variables (palette, spacing, font sizes, radii) and `src/styles/_mixins.scss` the shared mixins. Both are partials, so they never compile on their own.
- Components pull them in with `@use 'tokens' as *;` and `@use 'mixins' as *;`. The bare specifier resolves because `stylePreprocessorOptions.includePaths` points at `src/styles`.
- Component styles live next to their component as `*.component.scss` and stay encapsulated by the Angular view encapsulation, so a component only needs `@use` when it uses tokens or mixins.
- The `@schematics/angular:component` schematic of `angular.json` is set to `scss`, so `ng generate component` follows the same convention.

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
