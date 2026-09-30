import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ImportApiService } from '../../core/api/import-api.service';
import { ImportPreview, RowError } from '../../core/api/models';
import { ImportHistoryComponent } from './import-history.component';

@Component({
  selector: 'app-import-page',
  standalone: true,
  imports: [ImportHistoryComponent],
  template: `<h1>Import Wallet CSV</h1>
    <input type="file" accept=".csv,text/csv" (change)="select($event)">
    @if (message()) { <p role="alert">{{message()}}</p> }
    @if (canRetry()) { <button (click)="retry()">Re-preview</button> }
    @if (errors().length) { <p>Nothing was imported.</p><table><tr><th>Line</th><th>Column</th><th>Value</th><th>Message</th></tr>
      @for (error of errors(); track $index) { <tr><td>{{error.line}}</td><td>{{error.column}}</td><td>{{error.value}}</td><td>{{error.message}}</td></tr> }
    </table> }
    @if (preview(); as data) { <p>{{data.windowStart}}–{{data.windowEnd}} | Accounts: {{data.accounts.join(', ')}}</p>
      <p>{{data.unchangedCount}} unchanged, {{data.addedCount}} added, {{data.removedCount}} removed, {{data.changedCount}} changed</p>
      <label>Window start <input type="date" [value]="data.windowStart" #start></label>
      <label>Window end <input type="date" [value]="data.windowEnd" #end></label>
      <button (click)="rewindow(data.id, start.value, end.value)">Re-preview</button>
      @for (row of data.added; track $index) { <p>Added: {{row}}</p> }
      @for (row of data.removed; track $index) { <p>Removed: {{row}}</p> }
      @for (row of data.changed; track $index) { <p>Changed: {{row}}</p> }
      @if (data.identicalToBatchId) { <p>Identical to committed batch {{data.identicalToBatchId}}.</p> }
      <button (click)="commit(data.id)">Commit</button><button (click)="discard(data.id)">Discard</button>
    }
    <app-import-history />`
})
export class ImportPageComponent {
  private readonly api = inject(ImportApiService);
  private selectedFile: File | null = null;
  readonly preview = signal<ImportPreview | null>(null);
  readonly errors = signal<RowError[]>([]);
  readonly message = signal('');
  readonly canRetry = signal(false);
  select(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.selectedFile = file;
    this.errors.set([]); this.message.set('');
    this.api.stage(file).subscribe({ next: value => this.preview.set(value), error: (error: HttpErrorResponse) => {
      this.preview.set(null);
      this.errors.set(error.status === 422 ? (error.error?.errors ?? []) : []);
      if (error.status !== 422) this.message.set(error.message);
    } });
  }
  rewindow(id: number, start: string, end: string): void {
    this.api.rewindow(id, start, end).subscribe({ next: value => this.preview.set(value),
      error: (error: HttpErrorResponse) => { this.message.set(error.status === 410 ? 'This preview expired.' : error.message); this.canRetry.set(true); } });
  }
  commit(id: number): void {
    const data = this.preview();
    const confirmRemovals = data?.requiresRemovalConfirmation === true
      ? window.confirm(`This will supersede ${data.removedCount + data.changedCount} rows. Continue?`) : false;
    if (data?.requiresRemovalConfirmation && !confirmRemovals) return;
    this.api.commit(id, confirmRemovals).subscribe({ next: () => { this.preview.set(null); this.message.set('Import committed.'); },
      error: (error: HttpErrorResponse) => {
        this.message.set(error.status === 409 ? 'Data changed; re-preview before committing.' : error.status === 410 ? 'This preview expired.' : error.message);
        this.canRetry.set(error.status === 409 || error.status === 410);
      } });
  }
  retry(): void {
    if (!this.selectedFile) return;
    this.canRetry.set(false);
    this.api.stage(this.selectedFile).subscribe({ next: value => { this.preview.set(value); this.message.set(''); },
      error: (error: HttpErrorResponse) => this.message.set(error.message) });
  }
  discard(id: number): void { this.api.discard(id).subscribe(() => this.preview.set(null)); }
}
