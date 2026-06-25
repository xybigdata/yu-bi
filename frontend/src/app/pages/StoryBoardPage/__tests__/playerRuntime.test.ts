import type { RevealApi } from 'reveal.js';
import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetRevealRuntimeLoaderForTest,
  __setRevealRuntimeLoaderForTest,
  type RevealRuntimeModule,
} from '../revealRuntime';
import { createRevealPlayerRuntime } from '../playerRuntime';

const createRevealApi = () =>
  ({
    initialize: vi.fn(() => Promise.resolve({} as RevealApi)),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    destroy: vi.fn(),
  }) as unknown as RevealApi & {
    initialize: ReturnType<typeof vi.fn>;
    addEventListener: ReturnType<typeof vi.fn>;
    removeEventListener: ReturnType<typeof vi.fn>;
    destroy: ReturnType<typeof vi.fn>;
  };

const createRuntimeModule = (revealApi: RevealApi) => {
  const Reveal = vi.fn(function MockReveal() {
    return revealApi;
  });
  return {
    Reveal,
    RevealZoom: { id: 'zoom' },
  } as unknown as RevealRuntimeModule & {
    Reveal: ReturnType<typeof vi.fn>;
  };
};

const appendRevealRoot = (domId: string) => {
  const root = document.createElement('div');
  root.id = domId;
  document.body.append(root);
  return root;
};

describe('createRevealPlayerRuntime', () => {
  afterEach(() => {
    document.body.innerHTML = '';
    __resetRevealRuntimeLoaderForTest();
    vi.restoreAllMocks();
  });

  test('should create reveal player, bind slidechanged event and destroy runtime', async () => {
    appendRevealRoot('story-player');
    const revealApi = createRevealApi();
    const runtimeModule = createRuntimeModule(revealApi);
    __setRevealRuntimeLoaderForTest(vi.fn().mockResolvedValue(runtimeModule));
    const onSlideChanged = vi.fn() as EventListener;
    const onReady = vi.fn();

    const runtime = createRevealPlayerRuntime({
      domId: 'story-player',
      onSlideChanged,
      onReady,
      config: {
        hash: false,
        history: false,
        controls: true,
        autoSlide: 3000,
      },
    });

    await vi.waitFor(() => {
      expect(runtimeModule.Reveal).toHaveBeenCalled();
      expect(revealApi.initialize).toHaveBeenCalledTimes(1);
    });

    expect(runtimeModule.Reveal).toHaveBeenCalledWith(
      document.getElementById('story-player'),
      expect.objectContaining({
        hash: false,
        history: false,
        controls: true,
        autoSlide: 3000,
        plugins: [runtimeModule.RevealZoom],
      }),
    );
    expect(revealApi.addEventListener).toHaveBeenCalledWith(
      'slidechanged',
      onSlideChanged,
    );
    expect(onReady).toHaveBeenCalledWith(revealApi);
    expect(runtime.getReveal()).toBe(revealApi);

    runtime.destroy();

    expect(revealApi.removeEventListener).toHaveBeenCalledWith(
      'slidechanged',
      onSlideChanged,
    );
    expect(revealApi.destroy).toHaveBeenCalledTimes(1);
    expect(onReady).toHaveBeenLastCalledWith(null);
    expect(runtime.getReveal()).toBeNull();
  });

  test('should skip reveal creation when runtime is cancelled before loading completed', async () => {
    appendRevealRoot('story-player');
    const revealApi = createRevealApi();
    const runtimeModule = createRuntimeModule(revealApi);
    let resolveRuntime!: (runtime: RevealRuntimeModule) => void;
    __setRevealRuntimeLoaderForTest(
      vi.fn(
        () =>
          new Promise<RevealRuntimeModule>(resolve => {
            resolveRuntime = resolve;
          }),
      ),
    );

    const runtime = createRevealPlayerRuntime({
      domId: 'story-player',
      onSlideChanged: vi.fn() as EventListener,
      config: {
        hash: false,
      },
    });
    runtime.cancel();
    resolveRuntime(runtimeModule);

    await Promise.resolve();

    expect(runtimeModule.Reveal).not.toHaveBeenCalled();
    expect(revealApi.initialize).not.toHaveBeenCalled();
    expect(runtime.getReveal()).toBeNull();
  });
});
