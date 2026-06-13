import { CURRENCIES } from 'app/utils/currency';

describe('currency definitions', () => {
  it('should expose the supported currency codes in stable order', () => {
    expect(CURRENCIES.map(currency => currency.code)).toEqual([
      'CNY',
      'USD',
      'GBP',
      'AUD',
      'EUR',
      'JPY',
      'CAD',
    ]);
  });

  it('should preserve currency exponent metadata', () => {
    expect(CURRENCIES.find(currency => currency.code === 'JPY')).toEqual({
      base: 10,
      code: 'JPY',
      exponent: 0,
    });
  });
});
