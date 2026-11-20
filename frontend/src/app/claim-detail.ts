import { Component, effect, inject, signal, untracked } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, Audit, Claim, InfoRequest } from './api';

@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe],
  template: `
    <a routerLink="/">&larr; Back</a>
    @if (claim(); as c) {
      <h2>{{ c.claimNumber }} <span class="badge {{ c.status }}">{{ c.status }}</span></h2>
      <div class="card grid">
        <div><small>Claimant</small>{{ c.claimantName }}</div><div><small>Market / type</small>{{ c.market }} / {{ c.type }}</div>
        <div><small>Incident date</small>{{ c.incidentDate }}</div><div><small>Officer</small>{{ c.assignedOfficerId ?? 'unassigned' }}</div>
        <div><small>Estimate</small>{{ c.estimatedAmount | number: '1.2-2' }}</div>
        <div><small>Approved</small>{{ c.approvedAmount === null ? '–' : (c.approvedAmount | number: '1.2-2') }}</div>
        <div class="wide"><small>Description</small>{{ c.description }}</div>
        @if (c.decisionReason) { <div class="wide"><small>Decision reason</small>{{ c.decisionReason }}</div> }
      </div>

      <h3>Information requests</h3>
      @for (r of requests(); track r.id) {
        <div class="card">
          <b>Q:</b> {{ r.question }}<br />
          @if (r.answer) { <b>A:</b> {{ r.answer }} }
          @else if (isClaimant()) {
            <input placeholder="Your answer" [(ngModel)]="answers[r.id]" />
            <button (click)="answer(r)">Send answer</button>
          } @else { <i>Waiting for the claimant.</i> }
        </div>
      } @empty { <p>None.</p> }

      @if (isMine()) {
        <h3>Actions</h3>
        <div class="card form">
          @if (c.status === 'ASSIGNED') { <button (click)="run(api.startReview(c.id))">Start review</button> }
          @if (c.status === 'UNDER_REVIEW') {
            <input placeholder="Question for the claimant" [(ngModel)]="question" />
            <button (click)="requestInfo()">Request information</button>
            <hr />
            <input type="number" min="1" placeholder="Approved amount" [(ngModel)]="amount" />
            <input placeholder="Reason" [(ngModel)]="reason" />
            <button (click)="decide(true)">Approve</button>
            <button class="danger" (click)="decide(false)">Reject</button>
          }
          @if (c.status === 'APPROVED') { <button (click)="run(api.settle(c.id))">Mark as settled</button> }
          @if (['INFO_REQUESTED', 'REJECTED', 'SETTLED'].includes(c.status)) { <i>No actions available in this state.</i> }
        </div>
      } @else if (isOfficer() && c.status === 'SUBMITTED') {
        <button (click)="run(api.assign(c.id))">Assign to me</button>
      }

      <h3>Audit trail</h3>
      <ul>
        @for (a of audit(); track $index) {
          <li>{{ a.occurredAt | date: 'medium' }} &middot; <b>{{ a.action }}</b> by {{ a.actor }}{{ a.detail ? ' – ' + a.detail : '' }}</li>
        }
      </ul>
    }`,
})
export class ClaimDetail {
  api = inject(Api);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id')!;
  claim = signal<Claim | null>(null);
  requests = signal<InfoRequest[]>([]);
  audit = signal<Audit[]>([]);
  answers: Record<string, string> = {};
  question = ''; reason = ''; amount: number | null = null;

  constructor() {
    effect(() => { this.api.user(); untracked(() => this.refresh()); });
  }
  isClaimant() { return this.api.user().role === 'CLAIMANT'; }
  isOfficer() { return this.api.user().role === 'OFFICER'; }
  isMine() { return this.isOfficer() && this.claim()?.assignedOfficerId === this.api.user().id; }

  refresh() {
    this.api.get(this.id).subscribe({ next: (c) => this.claim.set(c), error: () => this.claim.set(null) });
    this.api.infoRequests(this.id).subscribe({ next: (r) => this.requests.set(r), error: () => this.requests.set([]) });
    this.api.audit(this.id).subscribe({ next: (a) => this.audit.set(a), error: () => this.audit.set([]) });
  }
  run(call: import('rxjs').Observable<unknown>) { call.subscribe({ next: () => this.refresh(), error: () => this.refresh() }); }
  requestInfo() { this.run(this.api.requestInfo(this.id, this.question)); this.question = ''; }
  answer(r: InfoRequest) { this.run(this.api.answer(this.id, r.id, this.answers[r.id] ?? '')); }
  decide(approved: boolean) { this.run(this.api.decide(this.id, approved, approved ? this.amount : null, this.reason)); }
}
