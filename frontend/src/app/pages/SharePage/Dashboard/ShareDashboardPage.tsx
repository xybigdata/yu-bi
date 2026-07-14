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

import useMount from 'app/hooks/useMount';
import useRouteQuery from 'app/hooks/useRouteQuery';
import ChartManager from 'app/models/ChartManager';
import { preloadChartPlugins } from 'app/services/chartPluginService';
import { useLocation } from 'app/routerCompat';
import { login } from 'app/slice/thunks';
import { ChartDataRequest } from 'app/features/query';
import {
  downloadShareDataChartFile,
  loadShareTask,
  makeShareDownloadDataTask,
} from 'app/utils/fetch';
import { useCallback, useEffect, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import styled from 'styled-components';
import { getToken } from 'utils/auth';
import persistence from 'utils/persistence';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';
import { BoardLoading } from '../../DashBoardPage/components/BoardLoading';
import { useBoardSlice } from '../../DashBoardPage/pages/Board/slice';
import { selectShareBoard } from '../../DashBoardPage/pages/Board/slice/selector';
import { VizRenderMode } from '../../DashBoardPage/pages/Board/slice/types';
import { useEditBoardSlice } from '../../DashBoardPage/pages/BoardEditor/slice';
import { FilterSearchParams } from '../../MainPage/pages/VizPage/slice/types';
import PasswordModal from '../components/PasswordModal';
import ShareLoginModal from '../components/ShareLoginModal';
import { shareActions, useShareSlice } from '../slice';
import {
  selectNeedVerify,
  selectShareExecuteTokenMap,
  selectShareVizType,
} from '../slice/selectors';
import { fetchShareVizInfo } from '../slice/thunks';
import { useShareRouteParams } from '../hooks/useShareRouteParams';
import DashboardForShare from './DashboardForShare';

function ShareDashboardPage() {
  const { shareActions: actions } = useShareSlice();
  useEditBoardSlice();
  useBoardSlice();

  const dispatch = useAppDispatch();
  const location = useLocation();
  const { token: shareToken } = useShareRouteParams('dashboard');
  const search = location.search;

  const executeTokenMap = useSelector(selectShareExecuteTokenMap);
  const needVerify = useSelector(selectNeedVerify);
  const shareBoard = useSelector(selectShareBoard);
  const vizType = useSelector(selectShareVizType);
  const logged = !!getToken();

  const shareType = useRouteQuery({
    key: 'type',
  });
  // in timed task eager=true for disable board lazyLoad
  const eager = useRouteQuery({
    key: 'eager',
  });
  const renderMode: VizRenderMode = eager ? 'schedule' : 'share';
  const searchParams = useMemo(() => {
    return urlSearchTransfer.toParams(search);
  }, [search]);

  useEffect(() => {
    if (shareBoard?.name) {
      dispatch(shareActions.savePageTitle({ title: shareBoard?.name }));
    }
  }, [shareBoard?.name, dispatch]);

  const loadVizData = () => {
    if (shareType === 'CODE') {
      const previousPassword = persistence.session.get(shareToken);

      if (previousPassword) {
        fetchShareVizInfoImpl(shareToken, previousPassword, searchParams);
      } else {
        dispatch(actions.saveNeedVerify(true));
      }
    } else if (shareType === 'LOGIN' && !logged) {
      dispatch(actions.saveNeedVerify(true));
    } else {
      fetchShareVizInfoImpl(shareToken, undefined, searchParams);
    }
  };
  useMount(() => {
    preloadChartPlugins()
      .then(() => loadVizData())
      .catch(err =>
        console.error('Fail to preload customize charts with ', err),
      );
  });

  const fetchShareVizInfoImpl = useCallback(
    (
      token?: string,
      pwd?: string,
      params?: FilterSearchParams,
      loginUser?: string,
      loginPwd?: string,
      authorizedToken?: string,
    ) => {
      dispatch(
        fetchShareVizInfo({
          shareToken: token,
          sharePassword: pwd,
          filterSearchParams: params,
          renderMode,
          userName: loginUser,
          passWord: loginPwd,
          authorizedToken,
        }),
      );
    },
    [dispatch, renderMode],
  );

  const onLoadShareTask = useMemo(() => {
    return () => loadShareTask(shareToken || '');
  }, [shareToken]);

  const onMakeShareDownloadDataTask = useCallback(
    (downloadParams: ChartDataRequest[], fileName: string) => {
      if (executeTokenMap && shareToken) {
        dispatch(
          makeShareDownloadDataTask({
            shareId: shareToken,
            executeToken: executeTokenMap,
            downloadParams: downloadParams,
            fileName: fileName,
            resolve: () => {
              dispatch(actions.setShareDownloadPolling(true));
            },
          }),
        );
      }
    },
    [shareToken, executeTokenMap, dispatch, actions],
  );

  const onDownloadFile = useCallback(
    task => {
      downloadShareDataChartFile({
        downloadId: task.id,
        shareId: shareToken || '',
      }).then(() => {
        dispatch(actions.setShareDownloadPolling(true));
      });
    },
    [shareToken, dispatch, actions],
  );

  const handleLogin = useCallback(
    values => {
      dispatch(
        login({
          params: values,
          resolve: () => {
            fetchShareVizInfoImpl(
              shareToken,
              undefined,
              searchParams,
              values.username,
              values.password,
            );
          },
        }),
      );
    },
    [dispatch, fetchShareVizInfoImpl, searchParams, shareToken],
  );

  return (
    <StyledWrapper className="yubi-viz">
      <ShareLoginModal
        open={Boolean(needVerify) && shareType === 'LOGIN'}
        onChange={handleLogin}
      />
      <PasswordModal
        open={Boolean(needVerify) && shareType === 'CODE'}
        onChange={sharePassword => {
          fetchShareVizInfoImpl(shareToken, sharePassword, searchParams);
        }}
      />
      {!vizType && !needVerify && (
        <div className="loading-container">
          <BoardLoading />
        </div>
      )}

      {!Boolean(needVerify) && shareToken && shareBoard && (
        <DashboardForShare
          dashboard={shareBoard}
          allowDownload={false}
          loadVizData={loadVizData}
          onMakeShareDownloadDataTask={onMakeShareDownloadDataTask}
          renderMode={renderMode}
          filterSearchUrl={''}
          onLoadShareTask={onLoadShareTask}
          onDownloadFile={onDownloadFile}
        />
      )}
    </StyledWrapper>
  );
}
export default ShareDashboardPage;
const StyledWrapper = styled.div`
  width: 100%;
  height: 100vh;
  .loading-container {
    display: flex;
    height: 100vh;
  }
`;
