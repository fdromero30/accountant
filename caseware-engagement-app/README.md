# CasewareEngagementApp

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.0.3.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Styles

The app is styled with [Sass](https://sass-lang.com/) (SCSS syntax, compiled by the Angular CLI):

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
