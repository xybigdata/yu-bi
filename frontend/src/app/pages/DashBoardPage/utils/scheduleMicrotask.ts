export const scheduleMicrotask = (task: () => void) => {
  if (typeof queueMicrotask === 'function') {
    queueMicrotask(task);
    return;
  }
  Promise.resolve().then(task);
};
