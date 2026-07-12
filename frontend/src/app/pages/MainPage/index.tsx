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

import type { ChartEditorBaseProps } from 'app/components/ChartEditor';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useMount from 'app/hooks/useMount';
import { useLocation } from 'app/routerCompat';
import { preloadChartPlugins } from 'app/services/chartPluginService';
import { useAppSlice } from 'app/slice';
import React, { useCallback, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAppDispatch } from 'app/hooks/useRedux';
import styled from 'styled-components';
import { defaultLazyLoad } from 'utils/loadable';
import { NotFoundPage } from '../NotFoundPage';
import { EditorPPT, PPTPlayer } from '../StoryBoardPage/Loadable';
import { AccessRoute } from './AccessRoute';
import { Background } from './Background';
import { Navbar } from './Navbar';
import {
  LazyAgentPage,
  LazyConfirmInvitePage,
  LazyMemberPage,
  LazyOrgSettingPage,
  LazyPermissionPage,
  LazyResourceMigrationPage,
  LazySchedulePage,
  LazySourcePage,
  LazyVariablePage,
  LazyViewPage,
  LazyVizPage,
} from './PageLoadables';
import { ResourceTypes } from './pages/PermissionPage/constants';
import { useViewSlice } from './pages/ViewPage/slice';
import { useVizSlice } from './pages/VizPage/slice';
import { initChartPreviewData } from './pages/VizPage/slice/thunks';
import { getChartEditorClosePath, MAIN_PAGE_ROUTE_PATHS } from './routes';
import { useMainSlice } from './slice';
import { selectOrgId } from './slice/selectors';
import {
  getDataProviders,
  getLoggedInUserPermissions,
  getUserSettings,
} from './slice/thunks';
import { MainPageRouteParams } from './types';

const LazyChartEditor = defaultLazyLoad(
  () => import('app/components/ChartEditor'),
  module => module.ChartEditor,
);

export function MainPage() {
  useAppSlice();
  const { actions } = useMainSlice();
  const { actions: vizActions } = useVizSlice();
  const { actions: viewActions } = useViewSlice();
  const dispatch = useAppDispatch();
  const orgId = useSelector(selectOrgId);
  const navigate = useCompatNavigate();
  // loaded first time

  useMount(
    () => {
      preloadChartPlugins().catch(err =>
        console.error('Fail to preload customize charts with ', err),
      );
      dispatch(getUserSettings(orgId));
      dispatch(getDataProviders());
    },
    () => {
      dispatch(actions.clear());
    },
  );

  useEffect(() => {
    if (orgId) {
      dispatch(vizActions.clear());
      dispatch(viewActions.clear());
      dispatch(getLoggedInUserPermissions(orgId));
    }
  }, [dispatch, vizActions, viewActions, orgId]);

  const onSaveInDataChart = useCallback(
    (orgId: string, backendChartId: string) => {
      dispatch(
        initChartPreviewData({
          backendChartId,
          orgId,
        }),
      );
      navigate.push(`/organizations/${orgId}/vizs/${backendChartId}`);
    },
    [dispatch, navigate],
  );

  const ChartEditorRoute = () => {
    const location = useLocation();
    const hisSearch = new URLSearchParams(location.search);
    const hisState = {
      dataChartId: hisSearch.get('dataChartId') || '',
      chartType: hisSearch.get('chartType') || 'dataChart',
      container: hisSearch.get('container') || 'dataChart',
      defaultViewId: hisSearch.get('defaultViewId') || '',
    } as ChartEditorBaseProps;
    const handleCloseChartEditor = () => {
      navigate.push(
        getChartEditorClosePath({
          orgId,
          dataChartId: hisState.dataChartId,
          defaultViewId: hisState.defaultViewId,
        }),
      );
    };

    return (
      <AccessRoute module={ResourceTypes.Viz}>
        <LazyChartEditor
          dataChartId={hisState.dataChartId}
          orgId={orgId}
          chartType={hisState.chartType}
          container={hisState.container}
          defaultViewId={hisState.defaultViewId}
          onClose={handleCloseChartEditor}
          onSaveInDataChart={onSaveInDataChart}
        />
      </AccessRoute>
    );
  };

  return (
    <AppContainer>
      <Background />
      <Navbar />
      {orgId && (
        <Routes>
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.root}
            element={<Navigate to={`/organizations/${orgId}`} replace />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.confirmInvite}
            element={<LazyConfirmInvitePage />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.organization}
            element={<Navigate to={`/organizations/${orgId}/vizs`} replace />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.agentWorkspace}
            element={<LazyAgentPage />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.vizChartEditor}
            element={<ChartEditorRoute />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.vizStoryPlayer}
            element={<PPTPlayer />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.vizStoryEditor}
            element={<EditorPPT />}
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.vizBoardEditor}
            element={
              <AccessRoute module={ResourceTypes.Viz}>
                <LazyVizPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.vizList}
            element={
              <AccessRoute module={ResourceTypes.Viz}>
                <LazyVizPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.vizDetail}
            element={
              <AccessRoute module={ResourceTypes.Viz}>
                <LazyVizPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.viewList}
            element={
              <AccessRoute module={ResourceTypes.View}>
                <LazyViewPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.viewDetail}
            element={
              <AccessRoute module={ResourceTypes.View}>
                <LazyViewPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.sourceList}
            element={
              <AccessRoute module={ResourceTypes.Source}>
                <LazySourcePage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.sourceDetail}
            element={
              <AccessRoute module={ResourceTypes.Source}>
                <LazySourcePage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.scheduleList}
            element={
              <AccessRoute module={ResourceTypes.Schedule}>
                <LazySchedulePage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.scheduleDetail}
            element={
              <AccessRoute module={ResourceTypes.Schedule}>
                <LazySchedulePage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.memberList}
            element={
              <AccessRoute module={ResourceTypes.User}>
                <LazyMemberPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.memberDetail}
            element={
              <AccessRoute module={ResourceTypes.User}>
                <LazyMemberPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.roleList}
            element={
              <AccessRoute module={ResourceTypes.User}>
                <LazyMemberPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.roleDetail}
            element={
              <AccessRoute module={ResourceTypes.User}>
                <LazyMemberPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.permissionList}
            element={
              <Navigate
                to={`/organizations/${orgId}/permissions/subject`}
                replace
              />
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.permissionViewpoint}
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyPermissionPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.permissionDetail}
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyPermissionPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.variables}
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyVariablePage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.orgSettings}
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyOrgSettingPage />
              </AccessRoute>
            }
          />
          <Route
            path={MAIN_PAGE_ROUTE_PATHS.resourceMigration}
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyResourceMigrationPage />
              </AccessRoute>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      )}
    </AppContainer>
  );
}

const AppContainer = styled.main`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  background-color: ${p => p.theme.bodyBackground};
`;
