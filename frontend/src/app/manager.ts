import { Component, effect, inject, signal, untracked } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api, Claim, Exposure, Workload } from './api';

@Component({
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  template: `
    <h2>Outstanding liability <button class="link" (click)="refresh()">Refresh</button></h2>
    @if (exposure(); as e) {
      <div class="cards">
        <div class="card"><small>Total outstanding</small><b>{{ e.totalOutstandingExposure | number: '1.2-2' }}</b></div>
        <div class="card"><small>Open claims</small><b>{{ e.openClaims }}</b></div>
      </div>
      <table>
        <tr><th>Market</th><th>Open claims</th><th>Outstanding exposure</th></tr>
        @for (m of e.byMarket; track m.market) {
          <tr><td>{{ m.market }}</td><td>{{ m.openClaims }}</td><td>{{ m.outstandingExposure | number: '1.2-2' }}</td></tr>
        } @empty { <tr><td colspan="3">No open claims.</td></tr> }
      </table>
      <p class="hint">Eventually consistent: built from Kafka events. As of {{ e.asOf }}.</p>
    }

    <h2>Officer workload</h2>
    <table>
      <tr><th>Officer</th><th>Open</th><th>Decided</th><th>Avg hours to decision</th></tr>
      @for (w of workload(); track w.officerId) {
        <tr><td>{{ w.officerId }}</td><td>{{ w.openClaims }}</td><td>{{ w.decidedClaims }}</td>
          <td>{{ w.avgHoursToDecision === null ? '–' : (w.avgHoursToDecision | number: '1.1-1') }}</td></tr>
      } @empty { <tr><td colspan="4">No assigned claims.</td></tr> }
    </table>

    <h2>All claims</h2>
    <table>
      <tr><th>Claim</th><th>Market</th><th>Estimate</th><th>Officer</th><th>Status</th></tr>
      @for (c of claims(); track c.id) {
        <tr><td><a [routerLink]="['/claims', c.id]">{{ c.claimNumber }}</a></td><td>{{ c.market }}</td>
          <td>{{ c.estimatedAmount | number: '1.2-2' }}</td><td>{{ c.assignedOfficerId ?? '–' }}</td>
          <td><span class="badge {{ c.status }}">{{ c.status }}</span></td></tr>
      }
    </table>`,
})
export class ManagerPage {
  private api = inject(Api);
  exposure = signal<Exposure | null>(null);
  workload = signal<Workload[]>([]);
  claims = signal<Claim[]>([]);

  constructor() {
    effect(() => { this.api.user(); untracked(() => this.refresh()); });
  }
  refresh() {
    this.api.exposure().subscribe({ next: (e) => this.exposure.set(e), error: () => this.exposure.set(null) });
    this.api.workload().subscribe({ next: (w) => this.workload.set(w), error: () => this.workload.set([]) });
    this.api.all().subscribe({ next: (c) => this.claims.set(c), error: () => this.claims.set([]) });
  }
}
