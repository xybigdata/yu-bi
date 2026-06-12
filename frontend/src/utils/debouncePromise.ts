type WaitValue = number | (() => number);

function getWait(wait: WaitValue) {
  return typeof wait === 'function' ? wait() : wait;
}

function createDeferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void;
  let reject!: (reason?: unknown) => void;

  const promise = new Promise<T>((innerResolve, innerReject) => {
    resolve = innerResolve;
    reject = innerReject;
  });

  return {
    promise,
    resolve,
    reject,
  };
}

export function debouncePromise<TArgs extends unknown[], TResult>(
  fn: (...args: TArgs) => TResult,
  wait: WaitValue = 0,
) {
  type ResolvedValue = Awaited<TResult>;

  let deferred: ReturnType<typeof createDeferred<ResolvedValue>> | null = null;
  let timer: ReturnType<typeof setTimeout> | null = null;
  let pendingArgs: TArgs | null = null;

  return (...args: TArgs) => {
    pendingArgs = args;

    if (timer) {
      clearTimeout(timer);
    }

    if (!deferred) {
      deferred = createDeferred<Awaited<TResult>>();
    }

    timer = setTimeout(() => {
      const currentDeferred = deferred;
      const currentArgs = pendingArgs;

      deferred = null;
      timer = null;
      pendingArgs = null;

      Promise.resolve(fn(...(currentArgs as TArgs)) as Awaited<TResult>).then(
        currentDeferred?.resolve,
        currentDeferred?.reject,
      );
    }, getWait(wait));

    return deferred.promise;
  };
}
