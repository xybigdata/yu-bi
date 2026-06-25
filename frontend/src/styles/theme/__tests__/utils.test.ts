import * as utils from '../utils';

describe('theme utils', () => {
  it('should get storage item', () => {
    utils.saveTheme('system');
    expect(utils.getThemeFromStorage()).toBe('system');
  });
  it('should check system theme', () => {
    expect([false, true, undefined]).toContain(utils.isSystemDark);
  });
});
