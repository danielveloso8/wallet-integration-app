import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { DashboardSummary, DashboardTrend } from './models';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  summary(from: string, to: string, accounts: string[]) {
    let params = new HttpParams().set('from', from).set('to', to);
    for (const account of accounts) params = params.append('accounts', account);
    return this.http.get<DashboardSummary>('/api/dashboard/summary', { params });
  }
  accounts() { return this.http.get<string[]>('/api/accounts'); }
  trend(endMonth: string, accounts: string[]) {
    let params = new HttpParams().set('endMonth', endMonth);
    for (const account of accounts) params = params.append('accounts', account);
    return this.http.get<DashboardTrend>('/api/dashboard/trend', { params });
  }
}
