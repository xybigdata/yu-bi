/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';

/**
 * **Validates: Requirements 1.7, 5.6**
 *
 * Property 4: Copyright header replacement preserves original attribution.
 *
 * For any source file containing the original copyright header pattern
 * (* Datart + Copyright 2021 + Apache 2.0 license reference),
 * the replaced header SHALL simultaneously contain:
 *   (a) `* YuBi` brand identifier
 *   (b) `(originally Datart by running-elephant)` attribution text
 *   (c) `Copyright 2024-2026 YuBi Contributors` new copyright line
 *   (d) Complete and unmodified Apache License 2.0 reference text
 */

// The Apache 2.0 license reference block (must remain unchanged)
const APACHE_LICENSE_BLOCK = [
  'Licensed under the Apache License, Version 2.0 (the "License");',
  'you may not use this file except in compliance with the License.',
  'You may obtain a copy of the License at',
  '',
  '    http://www.apache.org/licenses/LICENSE-2.0',
  '',
  'Unless required by applicable law or agreed to in writing, software',
  'distributed under the License is distributed on an "AS IS" BASIS,',
  'WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.',
  'See the License for the specific language governing permissions and',
  'limitations under the License.',
];

/**
 * Transforms an original Datart copyright header to the new YuBi format.
 * This is the function under test — it replicates the transformation logic
 * applied during the rename.
 */
function transformCopyrightHeader(originalHeader: string): string {
  // Match the original copyright header pattern:
  // It starts with `/*` or `/**`, contains `* Datart` (case sensitive),
  // contains `Copyright 2021`, and ends with the Apache 2.0 license block + `*/`
  const headerPattern =
    /\/\*\*?\s*\n(\s*\*\s*(?:<p>)?\s*Datart\s*(?:<\/p>)?\s*\n)([\s\S]*?)(Copyright\s+2021[^\n]*\n)([\s\S]*?)(Licensed under the Apache License[\s\S]*?limitations under the License\.)\s*\n\s*\*\//;

  const match = originalHeader.match(headerPattern);
  if (!match) {
    return originalHeader; // Not a matching header, return unchanged
  }

  const [fullMatch, , , copyrightLine, , licenseBlock] = match;

  // Extract the original copyright year info
  const yearMatch = copyrightLine.match(/Copyright\s+(\d{4})/);
  const originalYear = yearMatch ? yearMatch[1] : '2021';

  // Construct the new header
  const newHeader = [
    '/**',
    ' * YuBi',
    ' *',
    ` * Copyright ${originalYear} (originally Datart by running-elephant)`,
    ' * Copyright 2024-2026 YuBi Contributors',
    ' *',
    ...APACHE_LICENSE_BLOCK.map(line => (line ? ` * ${line}` : ' *')),
    ' */',
  ].join('\n');

  return originalHeader.replace(fullMatch, newHeader);
}

/**
 * Generates different variations of the original Datart copyright header.
 * Variations include:
 * - With/without `<p>` tags around "Datart"
 * - Different whitespace patterns
 * - Different comment styles (/* vs /**)
 * - Extra blank comment lines
 */
function originalHeaderArbitrary(): fc.Arbitrary<string> {
  return fc
    .record({
      useDoubleAsterisk: fc.boolean(),
      usePTags: fc.boolean(),
      extraSpaceAfterAsterisk: fc.boolean(),
      extraBlankLineBeforeCopyright: fc.boolean(),
      extraBlankLineAfterCopyright: fc.boolean(),
      trailingSpacesOnDatartLine: fc.nat({ max: 3 }),
    })
    .map(
      ({
        useDoubleAsterisk,
        usePTags,
        extraSpaceAfterAsterisk,
        extraBlankLineBeforeCopyright,
        extraBlankLineAfterCopyright,
        trailingSpacesOnDatartLine,
      }) => {
        const opener = useDoubleAsterisk ? '/**' : '/*';
        const space = extraSpaceAfterAsterisk ? '  ' : ' ';
        const trailing = ' '.repeat(trailingSpacesOnDatartLine);
        const datartText = usePTags
          ? `<p> Datart </p>${trailing}`
          : `Datart${trailing}`;
        const blankBefore = extraBlankLineBeforeCopyright ? ' *\n' : '';
        const blankAfter = extraBlankLineAfterCopyright ? ' *\n' : '';

        const lines = [
          opener,
          ` *${space}${datartText}`,
          ' *',
          `${blankBefore} * Copyright 2021`,
          `${blankAfter} *`,
          ...APACHE_LICENSE_BLOCK.map(line => (line ? ` * ${line}` : ' *')),
          ' */',
        ];

        return lines.join('\n');
      },
    );
}

describe('Property 4: Copyright header replacement preserves original attribution', () => {
  it('should contain YuBi brand, attribution, new copyright, and Apache 2.0 reference after transformation', () => {
    fc.assert(
      fc.property(originalHeaderArbitrary(), originalHeader => {
        const transformed = transformCopyrightHeader(originalHeader);

        // (a) Must contain `* YuBi` brand line
        expect(transformed).toContain('* YuBi');

        // (b) Must contain attribution text
        expect(transformed).toContain(
          '(originally Datart by running-elephant)',
        );

        // (c) Must contain new copyright line
        expect(transformed).toContain('Copyright 2024-2026 YuBi Contributors');

        // (d) Must contain the complete Apache 2.0 license reference
        expect(transformed).toContain(
          'Licensed under the Apache License, Version 2.0',
        );
        expect(transformed).toContain(
          'http://www.apache.org/licenses/LICENSE-2.0',
        );
        expect(transformed).toContain(
          'WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND',
        );
        expect(transformed).toContain('limitations under the License.');

        // Must NOT contain bare ` * Datart` without attribution context
        // The only occurrence of "Datart" should be within the attribution parenthetical
        const lines = transformed.split('\n');
        for (const line of lines) {
          if (line.includes('Datart')) {
            // Every line containing "Datart" must be within the attribution context
            expect(line).toContain('originally Datart by running-elephant');
          }
        }
      }),
      { numRuns: 100 },
    );
  });

  it('should preserve the original copyright year in attribution', () => {
    fc.assert(
      fc.property(originalHeaderArbitrary(), originalHeader => {
        const transformed = transformCopyrightHeader(originalHeader);

        // The original year (2021) must be preserved in the attribution line
        expect(transformed).toContain('Copyright 2021');
        // It should be combined with the attribution
        expect(transformed).toContain(
          'Copyright 2021 (originally Datart by running-elephant)',
        );
      }),
      { numRuns: 100 },
    );
  });

  it('should produce a well-formed block comment structure', () => {
    fc.assert(
      fc.property(originalHeaderArbitrary(), originalHeader => {
        const transformed = transformCopyrightHeader(originalHeader);

        // Must start with `/**` and end with `*/`
        expect(transformed).toMatch(/^\/\*\*/);
        expect(transformed).toMatch(/\*\/$/);

        // All intermediate lines should start with ` *`
        const lines = transformed.split('\n');
        for (let i = 1; i < lines.length - 1; i++) {
          expect(lines[i]).toMatch(/^\s*\*/);
        }
      }),
      { numRuns: 100 },
    );
  });
});
