type Listener<TArgs extends unknown[]> = (...args: TArgs) => void;

class BrowserEventBus {
  private listeners = new Map<string, Set<Listener<unknown[]>>>();

  on<TArgs extends unknown[]>(eventName: string, listener: Listener<TArgs>) {
    const eventListeners =
      this.listeners.get(eventName) || new Set<Listener<unknown[]>>();
    eventListeners.add(listener as Listener<unknown[]>);
    this.listeners.set(eventName, eventListeners);
  }

  emit<TArgs extends unknown[]>(eventName: string, ...args: TArgs) {
    const eventListeners = this.listeners.get(eventName);
    if (!eventListeners) {
      return;
    }
    eventListeners.forEach(listener => {
      (listener as Listener<TArgs>)(...args);
    });
  }

  off<TArgs extends unknown[]>(eventName: string, listener: Listener<TArgs>) {
    const eventListeners = this.listeners.get(eventName);
    if (!eventListeners) {
      return;
    }
    eventListeners.delete(listener as Listener<unknown[]>);
    if (eventListeners.size === 0) {
      this.listeners.delete(eventName);
    }
  }
}

const eventBus = new BrowserEventBus();

const WIDGET_MOVE = 'widgetMove';
const WIDGET_MOVE_END = 'widgetMoveEnd';
const BOARD_SCROLL = 'boardScroll';
interface FnWidgetMove {
  (selectedIdStr: string, deltaX: number, deltaY: number): void;
}

export const widgetMove = {
  on: (fn: FnWidgetMove) => {
    eventBus.on(WIDGET_MOVE, fn);
  },
  emit: (selectedIdStr: string, deltaX: number, deltaY: number) => {
    eventBus.emit(WIDGET_MOVE, selectedIdStr, deltaX, deltaY);
  },
  off: (fn: FnWidgetMove) => {
    eventBus.off(WIDGET_MOVE, fn);
  },
};
export const widgetMoveEnd = {
  on: (fn: () => void) => {
    eventBus.on(WIDGET_MOVE_END, fn);
  },
  emit: () => {
    eventBus.emit(WIDGET_MOVE_END);
  },
  off: (fn: () => void) => {
    eventBus.off(WIDGET_MOVE_END, fn);
  },
};
//
export const getScrollEvName = id => `${BOARD_SCROLL}_${id}`;
export const boardScroll = {
  on: (boardId: string, fn: () => void) => {
    eventBus.on(getScrollEvName(boardId), fn);
  },
  emit: (boardId: string) => {
    eventBus.emit(getScrollEvName(boardId));
  },
  off: (boardId: string, fn: () => void) => {
    eventBus.off(getScrollEvName(boardId), fn);
  },
};
