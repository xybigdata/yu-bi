/**
 * Datart
 *
 * Copyright 2021
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

import type Reveal from 'reveal.js';
import type { RevealPlugin } from 'reveal.js';
import 'reveal.js/reveal.css';

type RevealModule = typeof Reveal;
export type RevealRuntimeModule = {
  Reveal: RevealModule;
  RevealZoom: RevealPlugin;
};

let revealPromise: Promise<RevealRuntimeModule> | null = null;
let revealRuntimeLoader: () => Promise<RevealRuntimeModule> = () =>
  Promise.all([import('reveal.js'), import('reveal.js/plugin/zoom')]).then(
    ([revealModule, zoomModule]) => ({
      Reveal: revealModule.default,
      RevealZoom: zoomModule.default(),
    }),
  );

export function loadRevealRuntime() {
  if (!revealPromise) {
    revealPromise = revealRuntimeLoader().catch(error => {
      revealPromise = null;
      throw error;
    });
  }
  return revealPromise;
}

export function __setRevealRuntimeLoaderForTest(
  loader: () => Promise<RevealRuntimeModule>,
) {
  revealRuntimeLoader = loader;
  revealPromise = null;
}

export function __resetRevealRuntimeLoaderForTest() {
  revealRuntimeLoader = () =>
    Promise.all([import('reveal.js'), import('reveal.js/plugin/zoom')]).then(
      ([revealModule, zoomModule]) => ({
        Reveal: revealModule.default,
        RevealZoom: zoomModule.default(),
      }),
    );
  revealPromise = null;
}
