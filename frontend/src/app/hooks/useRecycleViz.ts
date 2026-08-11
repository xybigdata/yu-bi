/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

import { useMoveToRecycle } from 'app/features/recycle/useMoveToRecycle';
import { RecycleResourceType } from 'app/features/recycle/types';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import {
  getFolders,
  getStoryboards,
  removeTab,
} from 'app/pages/MainPage/pages/VizPage/slice/thunks';
import { VizType } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { useCallback } from 'react';
import { useAppDispatch } from 'app/hooks/useRedux';

type RecyclableVizType = Extract<VizType, RecycleResourceType>;

export const useRecycleViz = (
  orgId: string,
  vizId: string,
  type: RecyclableVizType,
) => {
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const redirect = useCallback(
    tabKey => {
      if (tabKey) {
        navigate.push(`/organizations/${orgId}/vizs/${tabKey}`);
      } else {
        navigate.push(`/organizations/${orgId}/vizs`);
      }
    },
    [navigate, orgId],
  );
  const handleCompleted = useCallback(async () => {
    if (type === 'STORYBOARD') {
      await dispatch(getStoryboards(orgId));
    } else {
      await dispatch(getFolders(orgId));
    }
    dispatch(removeTab({ id: vizId, resolve: redirect }));
  }, [dispatch, orgId, redirect, type, vizId]);
  const { moveToRecycle } = useMoveToRecycle({
    orgId,
    resourceType: type,
    onCompleted: handleCompleted,
  });
  const recycleViz = useCallback(
    () => moveToRecycle([vizId]),
    [moveToRecycle, vizId],
  );
  return recycleViz;
};
