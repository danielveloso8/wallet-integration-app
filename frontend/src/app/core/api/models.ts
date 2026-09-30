export interface RowError { line: number; column: string; value: string; message: string; }
export interface ImportPreview {
  id: number;
  windowStart: string;
  windowEnd: string;
  accounts: string[];
  unchangedCount: number;
  addedCount: number;
  removedCount: number;
  changedCount: number;
  added: unknown[];
  removed: unknown[];
  changed: unknown[];
  requiresRemovalConfirmation: boolean;
  identicalToBatchId?: number | null;
}
export interface ImportBatch { id: number; status: string; fileName: string; revertible: boolean; }
export interface MoneyValue { amount: string; currency: string; }
export interface DashboardSummary {
  from: string; to: string; prevFrom: string; prevTo: string;
  currencies: Array<{ currency: string; income: string; expense: string; net: string; transferIn: string; transferOut: string;
    incomeDelta: { abs: string; pct: string | null }; expenseDelta: { abs: string; pct: string | null }; netDelta: { abs: string; pct: string | null } }>;
  coverage: { complete: boolean; gaps: Array<{ account: string; start: string; end: string }> };
  previousCoverage: { complete: boolean; gaps: Array<{ account: string; start: string; end: string }> };
  categories: Array<{ currency: string; category: string; expense: string; income: string; prevExpense: string; prevIncome: string }>;
  transfers: Array<{ account: string; currency: string; in: string; out: string }>;
}
export interface TrendMonth { month: string; netByCurrency: Record<string, string>; covered: boolean; }
export interface DashboardTrend { months: TrendMonth[]; }
