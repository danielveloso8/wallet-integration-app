import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ImportApiService } from '../../core/api/import-api.service';
import { ImportBatch } from '../../core/api/models';

@Component({
  selector: 'app-import-history',
  standalone: true,
  template: `<h2>Import history</h2><table>
    <thead><tr><th>Batch</th><th>File</th><th>Status</th><th>Revert</th></tr></thead>
    <tbody>@for (batch of batches(); track batch.id) {
      <tr><td>{{batch.id}}</td><td>{{batch.fileName}}</td><td>{{batch.status}}</td>
        <td>@if (batch.revertible) { <button (click)="revert(batch)">Revert</button> }</td></tr>
    }</tbody></table>@if (message()) { <p role="alert">{{message()}}</p>}`
})
export class ImportHistoryComponent implements OnInit {
  private readonly api = inject(ImportApiService);
  readonly batches = signal<ImportBatch[]>([]);
  readonly message = signal('');
  ngOnInit(): void { this.refresh(); }
  refresh(): void { this.api.history().subscribe(rows => this.batches.set(rows)); }
  revert(batch: ImportBatch): void {
    if (!window.confirm(`Revert import ${batch.id}?`)) return;
    this.api.revert(batch.id).subscribe({ next: () => this.refresh(),
      error: (error: HttpErrorResponse) => this.message.set(error.message) });
  }
}
