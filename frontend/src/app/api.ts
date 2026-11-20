import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';

export type Role = 'CLAIMANT' | 'OFFICER' | 'MANAGER';
export interface User { id: string; role: Role; }
export const USERS: User[] = [
  { id: 'alice', role: 'CLAIMANT' }, { id: 'carol', role: 'CLAIMANT' },
  { id: 'bob', role: 'OFFICER' }, { id: 'dave', role: 'OFFICER' },
  { id: 'mgr', role: 'MANAGER' },
];

export interface Claim {
  id: string; claimNumber: string; claimantName: string; market: string; type: string;
  description: string; incidentDate: string; estimatedAmount: number; approvedAmount: number | null;
  decisionReason: string | null; status: string; assignedOfficerId: string | null;
  createdAt: string; updatedAt: string;
}
export interface InfoRequest { id: string; question: string; answer: string | null; requestedAt: string; }
export interface Audit { action: string; actor: string; detail: string | null; occurredAt: string; }
export interface Workload { officerId: string; openClaims: number; decidedClaims: number; avgHoursToDecision: number | null; byStatus: Record<string, number>; }
export interface Exposure {
  totalOutstandingExposure: number; openClaims: number;
  byMarket: { market: string; openClaims: number; outstandingExposure: number }[];
  byStatus: { status: string; count: number }[]; asOf: string;
}

const CLAIMS = 'http://localhost:8080/api';
const EXPOSURE = 'http://localhost:8081/api';

/** Stand-in for real auth: every request carries the selected identity. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const api = inject(Api);
  const u = api.user();
  api.error.set(null);
  return next(req.clone({ setHeaders: { 'X-User-Id': u.id, 'X-User-Role': u.role } })).pipe(
    catchError((e: HttpErrorResponse) => {
      api.error.set(e.status === 0 ? 'Cannot reach the server. Are both services running?'
        : e.error?.detail ?? `Request failed (${e.status})`);
      return throwError(() => e);
    }));
};

@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  user = signal<User>(USERS[0]);
  error = signal<string | null>(null);

  mine = () => this.http.get<Claim[]>(`${CLAIMS}/claims/mine`);
  queue = () => this.http.get<Claim[]>(`${CLAIMS}/claims/queue`);
  assigned = () => this.http.get<Claim[]>(`${CLAIMS}/claims/assigned`);
  all = () => this.http.get<Claim[]>(`${CLAIMS}/claims`);
  get = (id: string) => this.http.get<Claim>(`${CLAIMS}/claims/${id}`);
  audit = (id: string) => this.http.get<Audit[]>(`${CLAIMS}/claims/${id}/audit`);
  infoRequests = (id: string) => this.http.get<InfoRequest[]>(`${CLAIMS}/claims/${id}/info-requests`);
  workload = () => this.http.get<Workload[]>(`${CLAIMS}/staff/workload`);
  exposure = () => this.http.get<Exposure>(`${EXPOSURE}/exposure/summary`);

  submit = (body: unknown) => this.http.post<Claim>(`${CLAIMS}/claims`, body);
  assign = (id: string) => this.http.post<Claim>(`${CLAIMS}/claims/${id}/assign`, {});
  startReview = (id: string) => this.http.post<Claim>(`${CLAIMS}/claims/${id}/start-review`, {});
  requestInfo = (id: string, question: string) => this.http.post(`${CLAIMS}/claims/${id}/info-requests`, { question });
  answer = (id: string, rid: string, answer: string) => this.http.post(`${CLAIMS}/claims/${id}/info-requests/${rid}/answer`, { answer });
  decide = (id: string, approved: boolean, approvedAmount: number | null, reason: string) =>
    this.http.post<Claim>(`${CLAIMS}/claims/${id}/decision`, { approved, approvedAmount, reason });
  settle = (id: string) => this.http.post<Claim>(`${CLAIMS}/claims/${id}/settle`, {});
}
