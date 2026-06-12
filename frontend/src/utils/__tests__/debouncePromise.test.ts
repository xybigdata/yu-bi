import { debouncePromise } from 'utils/debouncePromise';

describe('debouncePromise', () => {
  it('should only resolve the latest call result within the debounce window', async () => {
    vi.useFakeTimers();

    const fn = vi.fn(async (value: string) => value.toUpperCase());
    const debounced = debouncePromise(fn, 200);

    const first = debounced('a');
    const second = debounced('b');

    await vi.advanceTimersByTimeAsync(200);

    await expect(first).resolves.toBe('B');
    await expect(second).resolves.toBe('B');
    expect(fn).toHaveBeenCalledTimes(1);
    expect(fn).toHaveBeenCalledWith('b');

    vi.useRealTimers();
  });

  it('should propagate rejection from the debounced function', async () => {
    vi.useFakeTimers();

    const error = new Error('failed');
    const fn = vi.fn(async (_value: string) => {
      throw error;
    });
    const debounced = debouncePromise(fn, 100);
    const pending = debounced('x');
    const rejection = expect(pending).rejects.toBe(error);

    await vi.advanceTimersByTimeAsync(100);

    await rejection;
    expect(fn).toHaveBeenCalledTimes(1);

    vi.useRealTimers();
  });
});
