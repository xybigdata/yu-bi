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
import {
  CloudDownloadOutlined,
  CopyFilled,
  DeleteOutlined,
  FileAddOutlined,
  ReloadOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps } from 'antd';
import { DownloadFileType } from 'app/constants';
import { ConfirmMenuLabel } from 'app/components/Popup';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSaveAsViz } from 'app/pages/MainPage/pages/VizPage/hooks/useSaveAsViz';
import { FC, memo, useCallback, useContext, useMemo, useState } from 'react';
import { useAppDispatch } from 'app/hooks/useRedux';
import { useRecycleViz } from '../../../../hooks/useRecycleViz';
import { usePublishBoard } from '../../hooks/usePublishBoard';
import { widgetsQueryAction } from '../../pages/Board/slice/asyncActions';
import { BoardActionContext } from '../ActionProvider/BoardActionProvider';
import { BoardContext } from '../BoardProvider/BoardProvider';

interface Props {
  onOpenShareLink: () => void;
  openStoryList: () => void;
  openMockData: () => void;
  onCloseDropdown?: () => void;
}
export const useBoardDropdownItems = ({
  onOpenShareLink,
  openStoryList,
  openMockData,
  onCloseDropdown,
}: Props) => {
  const t = useI18NPrefix(`viz.action`);
  const tg = useI18NPrefix(`global`);
  const [confirmKey, setConfirmKey] = useState<string>();
  const dispatch = useAppDispatch();
  const {
    allowDownload,
    allowShare,
    allowManage,
    renderMode,
    status,
    orgId,
    boardId,
  } = useContext(BoardContext);
  const recycleViz = useRecycleViz(orgId, boardId, 'DASHBOARD');
  const saveAsViz = useSaveAsViz();
  const { onBoardToDownLoad } = useContext(BoardActionContext);
  const { publishBoard } = usePublishBoard(boardId, 'DASHBOARD', status);
  const closeConfirmMenu = useCallback(() => {
    setConfirmKey(undefined);
    onCloseDropdown?.();
  }, [onCloseDropdown]);

  const openConfirmMenu = useCallback((key: string) => {
    setConfirmKey(key);
  }, []);

  const openConfirmMenuFromItem = useCallback(
    (key: string) =>
      (info: Parameters<NonNullable<MenuProps['onClick']>>[0]) => {
        info.domEvent.preventDefault();
        info.domEvent.stopPropagation();
        openConfirmMenu(key);
      },
    [openConfirmMenu],
  );

  return useMemo<MenuProps['items']>(() => {
    const reloadData = () => {
      dispatch(widgetsQueryAction({ boardId, renderMode }));
    };
    const confirmLabel = (
      key: string,
      label: string,
      onConfirm: (() => void) | undefined,
    ) => (
      <ConfirmMenuLabel
        open={confirmKey === key}
        title={t('common.confirm')}
        okText={t('common.ok')}
        cancelText={t('common.cancel')}
        onOpen={() => openConfirmMenu(key)}
        onClose={closeConfirmMenu}
        onConfirm={onConfirm}
      >
        {label}
      </ConfirmMenuLabel>
    );

    return [
      {
        key: 'reloadData',
        onClick: reloadData,
        icon: <ReloadOutlined />,
        label: t('syncData'),
      },
      ...(allowShare
        ? [
            { key: 'shareLinkLine', type: 'divider' as const },
            {
              key: 'shareLink',
              onClick: onOpenShareLink,
              icon: <ShareAltOutlined />,
              label: t('share.shareLink'),
            },
          ]
        : []),
      ...(allowDownload
        ? [
            { key: 'exportDataLine', type: 'divider' as const },
            {
              key: 'exportData',
              onClick: openConfirmMenuFromItem('exportData'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel('exportData', t('share.exportData'), () =>
                onBoardToDownLoad?.(DownloadFileType.Excel),
              ),
            },
            {
              key: 'exportPDF',
              onClick: openConfirmMenuFromItem('exportPDF'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel('exportPDF', t('share.exportPDF'), () =>
                onBoardToDownLoad?.(DownloadFileType.Pdf),
              ),
            },
            {
              key: 'exportPicture',
              onClick: openConfirmMenuFromItem('exportPicture'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(
                'exportPicture',
                t('share.exportPicture'),
                () => onBoardToDownLoad?.(DownloadFileType.Image),
              ),
            },
            {
              key: 'exportTpl',
              onClick: openConfirmMenuFromItem('exportTpl'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(
                'exportTpl',
                t('share.exportTpl'),
                openMockData,
              ),
            },
          ]
        : []),
      ...(allowManage
        ? [
            { key: 'unpublishLine', type: 'divider' as const },
            ...(status === 2
              ? [
                  {
                    key: 'unpublish',
                    onClick: publishBoard,
                    icon: <FileAddOutlined />,
                    label: t('unpublish'),
                  },
                ]
              : []),
            {
              key: 'saveAs',
              onClick: () => saveAsViz(boardId, 'DASHBOARD'),
              icon: <CopyFilled />,
              label: tg('button.saveAs'),
            },
            {
              key: 'addToStory',
              onClick: openStoryList,
              icon: <FileAddOutlined />,
              label: t('addToStory'),
            },
            {
              key: 'archive',
              onClick: recycleViz,
              icon: <DeleteOutlined />,
              label: tg('button.archive'),
            },
          ]
        : []),
    ];
  }, [
    allowDownload,
    allowManage,
    allowShare,
    boardId,
    dispatch,
    closeConfirmMenu,
    confirmKey,
    onBoardToDownLoad,
    onOpenShareLink,
    openMockData,
    openConfirmMenu,
    openConfirmMenuFromItem,
    openStoryList,
    publishBoard,
    recycleViz,
    renderMode,
    saveAsViz,
    status,
    t,
    tg,
  ]);
};

export const BoardDropdownList: FC<Props> = memo(props => {
  const menuItems = useBoardDropdownItems(props);
  return <Menu items={menuItems} />;
});
