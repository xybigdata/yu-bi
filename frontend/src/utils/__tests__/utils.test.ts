import { universalUUID, uuidv4 } from 'utils/utils';

const originalCrypto = globalThis.crypto;

describe('utils uuid', () => {
  afterEach(() => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: originalCrypto,
    });
  });

  it('should prefer crypto.randomUUID when available', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: {
        randomUUID: () => '11111111-1111-4111-8111-111111111111',
      },
    });

    expect(uuidv4()).toBe('11111111-1111-4111-8111-111111111111');
  });

  it('should build a v4 uuid from crypto.getRandomValues', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: {
        getRandomValues: (array: Uint8Array) => {
          array.set([
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0xaa,
            0xbb, 0xcc, 0xdd, 0xee, 0xff,
          ]);
          return array;
        },
      },
    });

    expect(uuidv4()).toBe('00112233-4455-4677-8899-aabbccddeeff');
  });

  it('should fall back when crypto is unavailable', () => {
    const randomSpy = vi
      .spyOn(Math, 'random')
      .mockReturnValueOnce(0)
      .mockReturnValueOnce(1 / 256)
      .mockReturnValueOnce(2 / 256)
      .mockReturnValueOnce(3 / 256)
      .mockReturnValueOnce(4 / 256)
      .mockReturnValueOnce(5 / 256)
      .mockReturnValueOnce(6 / 256)
      .mockReturnValueOnce(7 / 256)
      .mockReturnValueOnce(8 / 256)
      .mockReturnValueOnce(9 / 256)
      .mockReturnValueOnce(10 / 256)
      .mockReturnValueOnce(11 / 256)
      .mockReturnValueOnce(12 / 256)
      .mockReturnValueOnce(13 / 256)
      .mockReturnValueOnce(14 / 256)
      .mockReturnValueOnce(15 / 256);

    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: undefined,
    });

    expect(universalUUID()).toBe('00010203-0405-4607-8809-0a0b0c0d0e0f');

    randomSpy.mockRestore();
  });
});
