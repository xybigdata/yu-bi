import { vi } from 'vitest';
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

Object.defineProperty(globalThis, 'jest', {
  configurable: true,
  value: vi,
});

await import('./src/locales/i18n');
