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

import ChartEditor, { ChartEditorBaseProps } from 'app/components/ChartEditor';
import { CompatRoute } from 'app/components/CompatRoute';
import { CompatRedirect } from 'app/components/CompatRedirect';
import { CompatRoutes } from 'app/components/CompatRoutes';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useMount from 'app/hooks/useMount';
import ChartManager from 'app/models/ChartManager';
import { useLocation } from 'app/routerCompat';
import { useAppSlice } from 'app/slice';
import React, { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { NotFoundPage } from '../NotFoundPage';
import { StoryEditor } from '../StoryBoardPage/Editor';
import { StoryPlayer } from '../StoryBoardPage/Player';
import { AccessRoute } from './AccessRoute';
import { Background } from './Background';
import { Navbar } from './Navbar';
import { ConfirmInvitePage } from './pages/ConfirmInvitePage';
import { MemberPage } from './pages/MemberPage';
import { OrgSettingPage } from './pages/OrgSettingPage';
import { PermissionPage } from './pages/PermissionPage';
import { ResourceTypes } from './pages/PermissionPage/constants';
import { ResourceMigrationPage } from './pages/ResourceMigrationPage';
import { SchedulePage } from './pages/SchedulePage';
import { SourcePage } from './pages/SourcePage';
import { VariablePage } from './pages/VariablePage';
import { ViewPage } from './pages/ViewPage';
import { useViewSlice } from './pages/ViewPage/slice';
import { VizPage } from './pages/VizPage';
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

export function MainPage() {
  useAppSlice();
  const { actions } = useMainSlice();
  const { actions: vizActions } = useVizSlice();
  const { actions: viewActions } = useViewSlice();
  const dispatch = useDispatch();
  const orgId = useSelector(selectOrgId);
  const navigate = useCompatNavigate();
  // loaded first time

  useMount(
    () => {
      ChartManager.instance()
        .load()
        .catch(err =>
          console.error('Fail to load customize charts with ', err),
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
        <ChartEditor
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
        <CompatRoutes>
          <CompatRoute
            path="/"
            exact
            element={<CompatRedirect to={`/organizations/${orgId}`} />}
          />
          <CompatRoute
            path="/confirminvite"
            element={<ConfirmInvitePage />}
          />
          <CompatRoute
            path="/organizations/:orgId"
            exact
            element={<CompatRedirect to={`/organizations/${orgId}/vizs`} />}
          />
          <CompatRoute
            path="/organizations/:orgId/vizs/chartEditor"
            element={<ChartEditorRoute />}
          />
          <CompatRoute
            path="/organizations/:orgId/vizs/storyPlayer/:storyId"
            element={<StoryPlayer />}
          />
          <CompatRoute
            path="/organizations/:orgId/vizs/storyEditor/:storyId"
            element={<StoryEditor />}
          />
          <CompatRoute
            path="/organizations/:orgId/vizs/:vizId?"
            element={
              <AccessRoute module={ResourceTypes.Viz}>
                <VizPage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/views/:viewId?"
            element={
              <AccessRoute module={ResourceTypes.View}>
                <ViewPage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/sources"
            element={
              <AccessRoute module={ResourceTypes.Source}>
                <SourcePage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/schedules"
            element={
              <AccessRoute module={ResourceTypes.Schedule}>
                <SchedulePage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/members"
            element={
              <AccessRoute module={ResourceTypes.User}>
                <MemberPage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/roles"
            element={
              <AccessRoute module={ResourceTypes.User}>
                <MemberPage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/permissions"
            exact
            element={
              <CompatRedirect to={`/organizations/${orgId}/permissions/subject`} />
            }
          />
          <CompatRoute
            path="/organizations/:orgId/permissions/:viewpoint"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <PermissionPage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/variables"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <VariablePage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/orgSettings"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <OrgSettingPage />
              </AccessRoute>
            }
          />
          <CompatRoute
            path="/organizations/:orgId/resourceMigration"
            element={
              <AccessRoute module={ResourceTypes.Manager}>
                <ResourceMigrationPage />
              </AccessRoute>
            }
          />
          <CompatRoute path="*" element={<NotFoundPage />} />
        </CompatRoutes>
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
