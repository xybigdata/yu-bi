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
import {
  useLocation,
  useNavigate,
} from 'app/routerCompat';

type CompatLocationState = ReturnType<typeof useLocation>['state'];

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
  location: ReturnType<typeof useLocation>;
}

const normalizeTarget = (
  to: string | CompatLocationTarget,
  state?: CompatLocationState,
) => {
  if (typeof to === 'string') {
    return {
      to,
      state,
    };
  }

  const { state: targetState, ...path } = to;

  return {
    to: path,
    state: state === undefined ? targetState : state,
  };
};

export const useCompatNavigate = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const push = useCallback(
    (to: string | CompatLocationTarget, state?: CompatLocationState) => {
      const normalized = normalizeTarget(to, state);
      navigate(normalized.to, {
        state: normalized.state,
      });
    },
    [navigate],
  );
  const replace = useCallback(
    (to: string | CompatLocationTarget, state?: CompatLocationState) => {
      const normalized = normalizeTarget(to, state);
      navigate(normalized.to, {
        replace: true,
        state: normalized.state,
      });
    },
    [navigate],
  );
  const goBack = useCallback(() => {
    navigate(-1);
  }, [navigate]);
  const go = useCallback(
    (delta: number) => {
      navigate(delta);
    },
    [navigate],
  );

  return useMemo(
    () => ({
      push,
      replace,
      goBack,
      go,
      location,
    }),
    [go, goBack, location, push, replace],
  );
};
