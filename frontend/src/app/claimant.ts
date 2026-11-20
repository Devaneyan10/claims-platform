import { Component, effect, inject, signal, untracked } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, Claim } from './api';

@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, DecimalPipe],
  template: `
    <h2>Report an incident</h2>
    <div class="card form">
      <input placeholder="Your name" [(ngModel)]="f.claimantName" />
      <select [(ngModel)]="f.market">@for (m of markets; track m) { <option>{{ m }}</option> }</select>
      <select [(ngModel)]="f.type"><option>MOTOR</option><option>PROPERTY</option></select>
      <input type="date" [(ngModel)]="f.incidentDate" />
      <input type="number" min="1" placeholder="Estimated amount" [(ngModel)]="f.estimatedAmount" />
      <textarea placeholder="What happened?" [(ngModel)]="f.description"></textarea>
      <button (click)="submit()">Submit claim</button>
    </div>

    <h2>My claims</h2>
    <table>
      <tr><th>Claim</th><th>Type</th><th>Market</th><th>Estimate</th><th>Status</th></tr>
      @for (c of claims(); track c.id) {
        <tr><td><a [routerLink]="['/claims', c.id]">{{ c.claimNumber }}</a></td><td>{{ c.type }}</td>
          <td>{{ c.market }}</td><td>{{ c.estimatedAmount | number: '1.2-2' }}</td>
          <td><span class="badge {{ c.status }}">{{ c.status }}</span></td></tr>
      } @empty { <tr><td colspan="5">No claims yet.</td></tr> }
    </table>`,
})
export class ClaimantPage {
  private api = inject(Api);
  markets = ['SG', 'MY', 'HK', 'TH', 'ID', 'VN'];
  claims = signal<Claim[]>([]);
  f = this.blank();

  constructor() {
    effect(() => { this.api.user(); untracked(() => this.refresh()); });
  }
  private blank() {
    return { claimantName: '', market: 'SG', type: 'MOTOR', description: '',
      incidentDate: new Date().toISOString().slice(0, 10), estimatedAmount: null as number | null };
  }
  refresh() { this.api.mine().subscribe({ next: (c) => this.claims.set(c), error: () => this.claims.set([]) }); }
  submit() {
    this.api.submit(this.f).subscribe({ next: () => { this.f = this.blank(); this.refresh(); } });
  }
}
