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
import { EmptyFiller, Split } from 'app/components';
import { CompatRoute } from 'app/components/CompatRoute';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useLocation, useParams } from 'app/routerCompat';
import { useSplitSizes } from 'app/hooks/useSplitSizes';
import { useCallback, useMemo } from 'react';
import styled from 'styled-components/macro';
import { useMemberSlice } from '../MemberPage/slice';
import { useScheduleSlice } from '../SchedulePage/slice';
import { useSourceSlice } from '../SourcePage/slice';
import { useViewSlice } from '../ViewPage/slice';
import { useVizSlice } from '../VizPage/slice';
import { ResourceTypes, SubjectTypes, Viewpoints } from './constants';
import { Main } from './Main';
import { Sidebar } from './Sidebar';
import { usePermissionSlice } from './slice';

export function PermissionPage() {
  useMemberSlice();
  useScheduleSlice();
  useSourceSlice();
  useViewSlice();
  useVizSlice();
  usePermissionSlice();
  const { viewpoint } = useParams<{ viewpoint: Viewpoints }>();
  const location = useLocation();
  const detailParams = useMemo(() => {
    const urlArr = location.pathname.split('/');
    if (urlArr.length < 7 || urlArr[3] !== 'permissions') {
      return undefined;
    }
    return {
      type: urlArr[5] as ResourceTypes | SubjectTypes,
      id: urlArr[6],
    };
  }, [location.pathname]);
  const t = useI18NPrefix('permission');
  const { sizes, setSizes } = useSplitSizes({
    limitedSide: 0,
    range: [256, 768],
  });

  const siderDrag = useCallback(
    sizes => {
      setSizes(sizes);
    },
    [setSizes],
  );

  return (
    <Container
      sizes={sizes}
      minSize={[320, 0]}
      maxSize={[768, Infinity]}
      gutterSize={0}
      onDrag={siderDrag}
      className="datart-split"
    >
      <Sidebar
        viewpoint={viewpoint}
        viewpointType={detailParams?.type}
        viewpointId={detailParams?.id}
      />
      {detailParams ? (
        <CompatRoute
          path="/organizations/:orgId/permissions/:viewpoint/:type/:id"
          element={<Main />}
        />
      ) : (
        <EmptyFiller
          title={`${t('empty1')}${
            viewpoint === Viewpoints.Resource
              ? t('emptyResource')
              : t('emptySubject')
          }${t('empty2')}`}
        />
      )}
    </Container>
  );
}

const Container = styled(Split)`
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
`;
