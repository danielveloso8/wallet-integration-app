# Wallet CSV contract

Exports are UTF-8 (with or without a leading BOM), semicolon-delimited, and
RFC-4180 quoted. Headers may appear in any order but must contain each of the
following exactly once:

`date`, `type`, `amount`, `ref_currency_amount`, `currency`, `account`,
`category`, `payment_type`, `note`, `payee`, `labels`, `transfer`.

Values are preserved without trimming. `type` accepts only `Despesa` (expense)
and `Receita` (income); amounts are non-negative decimal values with at most
four fractional digits; currency is a three-letter uppercase code; and
transfer is exactly `true` or `false`. Dates are UTC instants formatted as
`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`. Account, category, and payment type must not be
blank.

Dates are grouped by calendar date in Europe/Lisbon. Imports replace the
snapshot for accounts present in the file and only within the selected window;
the default window spans the imported dates. Rows outside an explicitly chosen
window are rejected. Import preview precedes commit, which requires confirmation
when rows will be superseded.

Known limitations: only Portuguese type values are accepted; renaming an
account or category in Wallet splits its history; accounts with no rows in an
export are never replaced; refunds are not detected.
