import './__tests__/helper.chart';
import './__tests__/MockMatchMedia';

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = global.ResizeObserver || MockResizeObserver;
