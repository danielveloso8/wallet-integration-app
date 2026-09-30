import { Routes } from '@angular/router';
import { DashboardPageComponent } from './features/dashboard/dashboard-page.component';
import { ImportPageComponent } from './features/import/import-page.component';

export const appRoutes: Routes = [
  { path: '', component: DashboardPageComponent },
  { path: 'import', component: ImportPageComponent },
  { path: '**', redirectTo: '' }
];
