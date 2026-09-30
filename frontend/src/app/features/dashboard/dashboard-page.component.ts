import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { DashboardSummary, DashboardTrend } from '../../core/api/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { ChartComponent } from '../../shared/chart/chart.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [MoneyPipe, ChartComponent],
  template: `<h1>Wallet dashboard</h1>
    <label>Month <input type="month" [value]="month()" (change)="selectMonth($event)"></label>
    <label>From <input type="date" (change)="from.set($any($event.target).value)"></label>
    <label>To <input type="date" (change)="to.set($any($event.target).value)"></label>
    <label>Accounts <select multiple (change)="selectAccounts($event)">@for (account of accounts(); track account) { <option [value]="account">{{account}}</option> }</select></label>
    <button (click)="load()">Update</button>
    @if (summary(); as data) {
      <p>{{data.from}}–{{data.to}} (Europe/Lisbon) vs {{data.prevFrom}}–{{data.prevTo}}</p>
      @if (!data.coverage.complete || !data.previousCoverage.complete) {
        <aside>Coverage is incomplete.
          @for (gap of data.coverage.gaps; track $index) { <p>{{gap.account}}: {{gap.start}}–{{gap.end}}</p> }
          @for (gap of data.previousCoverage.gaps; track $index) { <p>Previous: {{gap.account}}: {{gap.start}}–{{gap.end}}</p> }
        </aside>
      }
      @for (group of data.currencies; track group.currency) {
        <section><h2>{{group.currency}}</h2>
          <p>Income: {{group.income | money: group.currency}} ({{group.incomeDelta.pct ?? '—'}}%)</p>
          <p>Expenses: {{group.expense | money: group.currency}} ({{group.expenseDelta.pct ?? '—'}}%)</p>
          <p>Net: {{group.net | money: group.currency}} ({{group.netDelta.pct ?? '—'}}%)</p>
          <p>Transfers in: {{group.transferIn | money: group.currency}}</p>
          <p>Transfers out: {{group.transferOut | money: group.currency}}</p>
          <h3>Categories</h3><table><tr><th>Category</th><th>Expense</th><th>Income</th></tr>
            @for (category of data.categories; track category.category) { @if (category.currency === group.currency) {
              <tr><td>{{category.category}}</td><td>{{category.expense | money: group.currency}}</td><td>{{category.income | money: group.currency}}</td></tr>
            } }
          </table>
        </section>
      }
      <h2>Transfers by account</h2><table>@for (transfer of data.transfers; track transfer.account + transfer.currency) {
        <tr><td>{{transfer.account}}</td><td>{{transfer.currency}}</td><td>{{transfer.in | money: transfer.currency}}</td><td>{{transfer.out | money: transfer.currency}}</td></tr>
      }</table>
      <h2>12-month trend</h2>
      @if (trend(); as series) { <app-chart type="bar" [data]="chartData()" /> }
    }`
})
export class DashboardPageComponent implements OnInit {
  private readonly api = inject(DashboardApiService);
  readonly summary = signal<DashboardSummary | null>(null);
  readonly trend = signal<DashboardTrend | null>(null);
  readonly accounts = signal<string[]>([]);
  readonly selectedAccounts = signal<string[]>([]);
  readonly month = signal('');
  readonly from = signal('');
  readonly to = signal('');
  readonly chartData = computed(() => {
    const months = this.trend()?.months ?? [];
    const currencies = [...new Set(months.flatMap(item => Object.keys(item.netByCurrency)))];
    return { labels: months.map(item => item.month), datasets: currencies.map(currency => ({
      label: currency,
      data: months.map(item => Number(item.netByCurrency[currency] ?? '0')),
      backgroundColor: months.map(item => item.covered ? '#2563eb' : '#cbd5e1')
    })) };
  });
  ngOnInit(): void {
    const month = new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Lisbon', year: 'numeric', month: '2-digit' }).format(new Date());
    this.from.set(`${month}-01`);
    this.month.set(month);
    const [year, monthNumber] = month.split('-').map(Number);
    const lastDay = new Date(Date.UTC(year, monthNumber, 0)).toISOString().slice(0, 10);
    this.to.set(lastDay);
    this.api.accounts().subscribe(value => this.accounts.set(value));
    this.load();
  }
  selectMonth(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.month.set(value);
    const [year, monthNumber] = value.split('-').map(Number);
    this.from.set(`${value}-01`);
    this.to.set(new Date(Date.UTC(year, monthNumber, 0)).toISOString().slice(0, 10));
  }
  selectAccounts(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedAccounts.set([...select.selectedOptions].map(option => option.value));
  }
  load(): void {
    this.api.summary(this.from(), this.to(), this.selectedAccounts()).subscribe(value => this.summary.set(value));
    this.api.trend(this.month(), this.selectedAccounts()).subscribe(value => this.trend.set(value));
  }
}
