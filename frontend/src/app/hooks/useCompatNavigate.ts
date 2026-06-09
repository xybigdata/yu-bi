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

import { useCallback, useMemo } from 'react';
import { useCompatHistory } from 'app/routerCompatRuntime';

type CompatHistory = ReturnType<typeof useCompatHistory>;
type CompatLocationState = CompatHistory['location']['state'];

interface CompatLocationTarget {
  hash?: string;
  pathname?: string;
  search?: string;
  state?: CompatLocationState;
}

export interface CompatNavigate {
  push: (to: string | CompatLocationTarget, state?: CompatLocationState) => void;
  replace: (
    to: string | CompatLocationTarget,
    state?: CompatLocationState,
  ) => void;
  goBack: () => void;
  go: (delta: number) => void;
  location: CompatHistory['location'];
}

export const useCompatNavigate = () => {
  const history = useCompatHistory();
  const push = useCallback(
    (to: string | CompatLocationTarget, state?: CompatLocationState) => {
      history.push(to, state);
    },
    [history],
  );
  const replace = useCallback(
    (to: string | CompatLocationTarget, state?: CompatLocationState) => {
      history.replace(to, state);
    },
    [history],
  );
  const goBack = useCallback(() => {
    history.goBack();
  }, [history]);
  const go = useCallback(
    (delta: number) => {
      history.go(delta);
    },
    [history],
  );

  return useMemo(
    () => ({
      push,
      replace,
      goBack,
      go,
      location: history.location,
    }),
    [go, goBack, history.location, push, replace],
  );
};
