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

    return (
      <AccessRoute module={ResourceTypes.Viz}>
        <LazyChartEditor
          dataChartId={hisState.dataChartId}
          orgId={orgId}
          chartType={hisState.chartType}
          container={hisState.container}
          defaultViewId={hisState.defaultViewId}
          onClose={() => navigate.go(-1)}
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
            path="/"
            element={<Navigate to={`/organizations/${orgId}`} replace />}
          />
          <Route path="/confirminvite" element={<LazyConfirmInvitePage />} />
          <Route
            path="/organizations/:orgId"
            element={<Navigate to={`/organizations/${orgId}/vizs`} replace />}
          />
          <Route
            path="/organizations/:orgId/vizs/chartEditor"
            element={<ChartEditorRoute />}
          />
          <Route
            path="/organizations/:orgId/vizs/storyPlayer/:storyId"
            element={<PPTPlayer />}
          />
          <Route
            path="/organizations/:orgId/vizs/storyEditor/:storyId"
            element={<EditorPPT />}
          />
          <Route
            path="/organizations/:orgId/vizs/:vizId?"
            element={
              <AccessRoute module={ResourceTypes.Viz}>
                <LazyVizPage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/views/:viewId?"
            element={
              <AccessRoute module={ResourceTypes.View}>
                <LazyViewPage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/sources"
            element={
              <AccessRoute module={ResourceTypes.Source}>
                <LazySourcePage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/schedules/:scheduleId?"
            element={
              <AccessRoute module={ResourceTypes.Schedule}>
                <LazySchedulePage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/members"
            element={
              <AccessRoute module={ResourceTypes.User}>
                <LazyMemberPage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/roles"
            element={
              <AccessRoute module={ResourceTypes.User}>
                <LazyMemberPage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/permissions"
            element={
              <Navigate
                to={`/organizations/${orgId}/permissions/subject`}
                replace
              />
            }
          />
          <Route
            path="/organizations/:orgId/permissions/:viewpoint"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyPermissionPage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/variables"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyVariablePage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/orgSettings"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <LazyOrgSettingPage />
              </AccessRoute>
            }
          />
          <Route
            path="/organizations/:orgId/resourceMigration"
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
