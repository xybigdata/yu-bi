import '@testing-library/jest-dom/extend-expect';
import 'locales/i18n';
import './__tests__/helper.chart';
import './__tests__/MockMatchMedia';

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = global.ResizeObserver || MockResizeObserver;
