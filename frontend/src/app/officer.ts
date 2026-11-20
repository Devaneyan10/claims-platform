import { Component, effect, inject, signal, untracked } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api, Claim, Workload } from './api';

@Component({
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  template: `
    <h2>My workload</h2>
    @for (w of workload(); track w.officerId) {
      <div class="cards">
        <div class="card"><small>Open claims</small><b>{{ w.openClaims }}</b></div>
        <div class="card"><small>Decided</small><b>{{ w.decidedClaims }}</b></div>
        <div class="card"><small>Avg hours to decision</small><b>{{ w.avgHoursToDecision === null ? '–' : (w.avgHoursToDecision | number: '1.1-1') }}</b></div>
      </div>
    } @empty { <p>Nothing assigned to you yet.</p> }

    <h2>Unassigned queue (oldest first)</h2>
    <table>
      <tr><th>Claim</th><th>Market</th><th>Type</th><th>Estimate</th><th>Submitted</th><th></th></tr>
      @for (c of queue(); track c.id) {
        <tr><td><a [routerLink]="['/claims', c.id]">{{ c.claimNumber }}</a></td><td>{{ c.market }}</td><td>{{ c.type }}</td>
          <td>{{ c.estimatedAmount | number: '1.2-2' }}</td><td>{{ c.createdAt.slice(0, 10) }}</td>
          <td><button (click)="assign(c)">Assign to me</button></td></tr>
      } @empty { <tr><td colspan="6">Queue is empty.</td></tr> }
    </table>

    <h2>My claims</h2>
    <table>
      <tr><th>Claim</th><th>Market</th><th>Estimate</th><th>Status</th></tr>
      @for (c of mine(); track c.id) {
        <tr><td><a [routerLink]="['/claims', c.id]">{{ c.claimNumber }}</a></td><td>{{ c.market }}</td>
          <td>{{ c.estimatedAmount | number: '1.2-2' }}</td><td><span class="badge {{ c.status }}">{{ c.status }}</span></td></tr>
      } @empty { <tr><td colspan="4">None.</td></tr> }
    </table>`,
})
export class OfficerPage {
  private api = inject(Api);
  queue = signal<Claim[]>([]);
  mine = signal<Claim[]>([]);
  workload = signal<Workload[]>([]);

  constructor() {
    effect(() => { this.api.user(); untracked(() => this.refresh()); });
  }
  refresh() {
    this.api.queue().subscribe({ next: (c) => this.queue.set(c), error: () => this.queue.set([]) });
    this.api.assigned().subscribe({ next: (c) => this.mine.set(c), error: () => this.mine.set([]) });
    this.api.workload().subscribe({ next: (w) => this.workload.set(w), error: () => this.workload.set([]) });
  }
  assign(c: Claim) { this.api.assign(c.id).subscribe({ next: () => this.refresh(), error: () => this.refresh() }); }
}
