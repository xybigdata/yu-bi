import { css } from 'styled-components';
import { media, sizes } from '../media';

describe('media', () => {
  it('should return media query in css', () => {
    const normalizeCss = (value: string) => value.replace(/\s+/g, '');
    const mediaQuery = normalizeCss(media.small`color: red;`.join(''));
    const cssVersion = css`
      @media (min-width: ${sizes.small}px) {
        color: red;
      }
    `.join('');
    expect(mediaQuery).toEqual(normalizeCss(cssVersion));
  });
});
