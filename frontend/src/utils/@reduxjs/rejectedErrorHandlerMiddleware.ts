/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createListenerMiddleware, isRejected } from '@reduxjs/toolkit';
import { message } from 'antd';

const rejectedErrorHandlerMiddleware = createListenerMiddleware();

const getMessage = (value: unknown): string | undefined => {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const record = value as { message?: unknown };
  return typeof record.message === 'string' ? record.message : undefined;
};

const getRejectedErrorMessage = (action: unknown): string | undefined => {
  if (!action || typeof action !== 'object') {
    return undefined;
  }
  const record = action as { payload?: unknown; error?: unknown };
  return getMessage(record.payload) || getMessage(record.error);
};

rejectedErrorHandlerMiddleware.startListening({
  predicate: isRejected,
  effect: async (action, listenerApi) => {
    listenerApi.cancelActiveListeners();
    await listenerApi.delay(100);

    const errorMessage = getRejectedErrorMessage(action);
    if (errorMessage) {
      message.error(errorMessage);
    }
    console.error(`Redux Rejection Error | `, action);
  },
});

export default rejectedErrorHandlerMiddleware;
