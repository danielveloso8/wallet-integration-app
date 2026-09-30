import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterOutlet],
  template: `<nav><a routerLink="/">Dashboard</a><a routerLink="/import">Import</a></nav><main><router-outlet /></main>`
})
export class AppComponent {}
