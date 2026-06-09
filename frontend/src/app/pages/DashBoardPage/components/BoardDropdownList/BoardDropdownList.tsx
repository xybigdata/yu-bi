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
import {
  CloudDownloadOutlined,
  CopyFilled,
  DeleteOutlined,
  FileAddOutlined,
  ReloadOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps, Popconfirm } from 'antd';
import { DownloadFileType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSaveAsViz } from 'app/pages/MainPage/pages/VizPage/hooks/useSaveAsViz';
import { FC, memo, useContext } from 'react';
import { useDispatch } from 'react-redux';
import { useRecycleViz } from '../../../../hooks/useRecycleViz';
import { usePublishBoard } from '../../hooks/usePublishBoard';
import { widgetsQueryAction } from '../../pages/Board/slice/asyncActions';
import { BoardActionContext } from '../ActionProvider/BoardActionProvider';
import { BoardContext } from '../BoardProvider/BoardProvider';

interface Props {
  onOpenShareLink: () => void;
  openStoryList: () => void;
  openMockData: () => void;
}
export const BoardDropdownList: FC<Props> = memo(
  ({ onOpenShareLink, openStoryList, openMockData }) => {
    const t = useI18NPrefix(`viz.action`);
    const tg = useI18NPrefix(`global`);
    const dispatch = useDispatch();
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
    const reloadData = () => {
      dispatch(widgetsQueryAction({ boardId, renderMode }));
    };
    const { onBoardToDownLoad } = useContext(BoardActionContext);
    const { publishBoard } = usePublishBoard(boardId, 'DASHBOARD', status);
    const confirmLabel = (
      label: string,
      onConfirm: () => void | undefined,
    ) => (
      <Popconfirm
        placement="left"
        title={t('common.confirm')}
        okText={t('common.ok')}
        cancelText={t('common.cancel')}
        onConfirm={onConfirm}
      >
        {label}
      </Popconfirm>
    );
    const menuItems: MenuProps['items'] = [
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
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportData'), () =>
                onBoardToDownLoad?.(DownloadFileType.Excel),
              ),
            },
            {
              key: 'exportPDF',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportPDF'), () =>
                onBoardToDownLoad?.(DownloadFileType.Pdf),
              ),
            },
            {
              key: 'exportPicture',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportPicture'), () =>
                onBoardToDownLoad?.(DownloadFileType.Image),
              ),
            },
            {
              key: 'exportTpl',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportTpl'), openMockData),
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
    return <Menu items={menuItems} />;
  },
);
