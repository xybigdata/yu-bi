import '@testing-library/jest-dom/vitest';
import './src/setupTests';

const storage = new Map<string, string>();
const localStorageMock = {
  clear: () => storage.clear(),
  getItem: (key: string) => storage.get(key) ?? null,
  key: (index: number) => Array.from(storage.keys())[index] ?? null,
  removeItem: (key: string) => storage.delete(key),
  setItem: (key: string, value: string) => storage.set(key, String(value)),
  get length() {
    return storage.size;
  },
};

Object.defineProperty(globalThis, 'localStorage', {
  configurable: true,
  value: localStorageMock,
});

Object.defineProperty(window, 'localStorage', {
  configurable: true,
  value: localStorageMock,
});

const nativeGetComputedStyle = window.getComputedStyle.bind(window);
Object.defineProperty(window, 'getComputedStyle', {
  configurable: true,
  value: (element: Element) => nativeGetComputedStyle(element),
});

const createCanvasContext = () =>
  ({
    clearRect: () => {},
    createImageData: () => ({ data: new Uint8ClampedArray(4) }),
    createLinearGradient: () => ({ addColorStop: () => {} }),
    drawImage: () => {},
    fillRect: () => {},
    fillText: () => {},
    getImageData: () => ({ data: new Uint8ClampedArray(4) }),
    measureText: () => ({ width: 0 }),
    putImageData: () => {},
    restore: () => {},
    rotate: () => {},
    save: () => {},
    scale: () => {},
    setTransform: () => {},
    translate: () => {},
  }) as unknown as CanvasRenderingContext2D;

Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
  configurable: true,
  value: (contextId: string) =>
    contextId === '2d' ? createCanvasContext() : null,
});

await import('./src/locales/i18n');
