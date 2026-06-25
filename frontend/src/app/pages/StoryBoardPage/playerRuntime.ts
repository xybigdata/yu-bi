import type { RevealApi, RevealConfig } from 'reveal.js';
import { loadRevealRuntime } from './revealRuntime';

type SlideChangedListener = Parameters<RevealApi['addEventListener']>[1];

export type RevealPlayerRuntimeOptions = {
  domId: string;
  config: Omit<RevealConfig, 'plugins'>;
  onSlideChanged: SlideChangedListener;
  onReady?: (reveal: RevealApi | null) => void;
};

export type RevealPlayerRuntimeHandle = {
  cancel: () => void;
  destroy: () => void;
  getReveal: () => RevealApi | null;
};

export function createRevealPlayerRuntime({
  domId,
  config,
  onSlideChanged,
  onReady,
}: RevealPlayerRuntimeOptions): RevealPlayerRuntimeHandle {
  let cancelled = false;
  let reveal: RevealApi | null = null;

  void loadRevealRuntime()
    .then(({ Reveal, RevealZoom }) => {
      if (cancelled) {
        return;
      }
      reveal = new Reveal(document.getElementById(domId)!, {
        ...config,
        plugins: [RevealZoom],
      });
      void reveal.initialize();
      reveal.addEventListener('slidechanged', onSlideChanged);
      onReady?.(reveal);
    })
    .catch(error => {
      console.error('Load story player runtime failed', error);
    });

  return {
    cancel: () => {
      cancelled = true;
    },
    destroy: () => {
      reveal?.removeEventListener('slidechanged', onSlideChanged);
      reveal?.destroy();
      reveal = null;
      onReady?.(null);
    },
    getReveal: () => reveal,
  };
}
