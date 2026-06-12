import {
  getToken,
  removeToken,
  setToken,
  setTokenExpiration,
} from 'utils/auth';

describe('auth cookie helpers', () => {
  beforeEach(() => {
    document.cookie = 'AUTHORIZATION_TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    setTokenExpiration(1000 * 60 * 60);
  });

  it('should set and get token with root path', () => {
    setToken('demo-token');

    expect(document.cookie).toContain('AUTHORIZATION_TOKEN=demo-token');
    expect(getToken()).toBe('demo-token');
  });

  it('should remove token', () => {
    setToken('demo-token');
    removeToken();

    expect(getToken()).toBeUndefined();
  });

  it('should honor custom expiration when setting token', () => {
    const cookieSetter = vi.spyOn(document, 'cookie', 'set');

    setTokenExpiration(1000);
    setToken('demo-token');

    expect(cookieSetter).toHaveBeenCalledWith(
      expect.stringContaining('path=/'),
    );
    expect(cookieSetter).toHaveBeenCalledWith(
      expect.stringContaining('expires='),
    );

    cookieSetter.mockRestore();
  });
});
