import { urlSearchTransfer } from 'utils/urlSearchTransfer';

describe('urlSearchTransfer', () => {
  it('should parse repeated query params as string arrays', () => {
    expect(urlSearchTransfer.toParams('foo=1&foo=2&bar=true')).toEqual({
      foo: ['1', '2'],
      bar: ['true'],
    });
  });

  it('should serialize scalar and array query params', () => {
    expect(
      urlSearchTransfer.toUrlString({
        foo: ['1', '2'],
        bar: true,
        count: 3,
      }),
    ).toBe('foo=1&foo=2&bar=true&count=3');
  });
});
