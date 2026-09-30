import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ImportBatch, ImportPreview } from './models';

@Injectable({ providedIn: 'root' })
export class ImportApiService {
  private readonly http = inject(HttpClient);
  stage(file: File, windowStart?: string, windowEnd?: string) {
    const form = new FormData(); form.set('file', file);
    if (windowStart) form.set('windowStart', windowStart);
    if (windowEnd) form.set('windowEnd', windowEnd);
    return this.http.post<ImportPreview>('/api/imports', form);
  }
  rewindow(id: number, windowStart: string, windowEnd: string) {
    return this.http.patch<ImportPreview>(`/api/imports/${id}`, { windowStart, windowEnd });
  }
  commit(id: number, confirmRemovals: boolean) {
    return this.http.post<ImportBatch>(`/api/imports/${id}/commit`, { confirmRemovals });
  }
  discard(id: number) { return this.http.delete<void>(`/api/imports/${id}`); }
  history() { return this.http.get<ImportBatch[]>('/api/imports'); }
  revert(id: number) { return this.http.post<ImportBatch>(`/api/imports/${id}/revert`, {}); }
}
