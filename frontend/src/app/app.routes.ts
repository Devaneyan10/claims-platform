import { Routes } from '@angular/router';
import { ClaimantPage } from './claimant';
import { OfficerPage } from './officer';
import { ManagerPage } from './manager';
import { ClaimDetail } from './claim-detail';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'claimant' },
  { path: 'claimant', component: ClaimantPage },
  { path: 'officer', component: OfficerPage },
  { path: 'manager', component: ManagerPage },
  { path: 'claims/:id', component: ClaimDetail },
];
