import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  it('formats decimal text as currency for display', () => {
    expect(new MoneyPipe().transform('0.1', 'EUR'))
      .toBe(new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' }).format(0.1));
  });
});
