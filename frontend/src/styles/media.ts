/*
 * Media queries utility
 */

import {
  css,
  CSSObject,
  DefaultTheme,
  Interpolation,
  RuleSet,
  StyleFunction,
} from 'styled-components';

/*
 * Taken from https://github.com/DefinitelyTyped/DefinitelyTyped/issues/32914
 */

// Update your breakpoints if you want
export const sizes = {
  small: 600,
  medium: 1024,
  large: 1440,
  xlarge: 1920,
};

// Iterate through the sizes and create a media template
export const media = (Object.keys(sizes) as Array<keyof typeof sizes>).reduce(
  (acc, label) => {
    acc[label] = (first: any, ...interpolations: any[]) => css`
      @media (min-width: ${sizes[label]}px) {
        ${css(first, ...interpolations)}
      }
    `;

    return acc;
  },
  {} as { [key in keyof typeof sizes]: MediaFunction },
);

/*
 * The helper keeps a stable typed API for media query templates across
 * styled-components major versions.
 */
type MediaFunction = <P extends object>(
  first:
    | TemplateStringsArray
    | CSSObject
    | StyleFunction<P & { theme: DefaultTheme }>,
  ...interpolations: Array<Interpolation<P & { theme: DefaultTheme }>>
) => RuleSet<P & { theme: DefaultTheme }>;

/* Example
const SomeDiv = styled.div`
  display: flex;
  ....
  ${media.medium`
    display: block
  `}
`;
*/
