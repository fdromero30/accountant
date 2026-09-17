import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Engagements · Caseware',
    loadComponent: () =>
      import('./features/engagement-list/engagement-list.component').then(
        (module) => module.EngagementListComponent
      )
  },
  { path: '**', redirectTo: '' }
];
