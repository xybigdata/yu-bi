export interface CurrencyDefinition {
  base: number;
  code: string;
  exponent: number;
}

export const CURRENCIES: CurrencyDefinition[] = [
  { code: 'CNY', base: 10, exponent: 2 },
  { code: 'USD', base: 10, exponent: 2 },
  { code: 'GBP', base: 10, exponent: 2 },
  { code: 'AUD', base: 10, exponent: 2 },
  { code: 'EUR', base: 10, exponent: 2 },
  { code: 'JPY', base: 10, exponent: 0 },
  { code: 'CAD', base: 10, exponent: 2 },
];
