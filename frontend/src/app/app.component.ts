import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Api, USERS } from './api';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, FormsModule],
  template: `
    <header>
      <strong>Claims Platform</strong>
      <label>Signed in as
        <select [ngModel]="api.user().id" (ngModelChange)="switch($event)">
          @for (u of users; track u.id) { <option [value]="u.id">{{ u.id }} ({{ u.role }})</option> }
        </select>
      </label>
    </header>
    @if (api.error(); as e) { <div class="error" role="alert">{{ e }}</div> }
    <main><router-outlet /></main>`,
})
export class AppComponent {
  api = inject(Api);
  private router = inject(Router);
  users = USERS;
  switch(id: string) {
    const u = USERS.find((x) => x.id === id)!;
    this.api.user.set(u);
    this.router.navigateByUrl('/' + u.role.toLowerCase());
  }
}
