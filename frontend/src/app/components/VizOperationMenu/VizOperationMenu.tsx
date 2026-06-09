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
  VerticalAlignBottomOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps, Popconfirm } from 'antd';
import { DownloadFileType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, memo } from 'react';
import styled from 'styled-components';

const VizOperationMenu: FC<{
  onShareLinkClick?;
  onDownloadDataLinkClick?;
  onSaveAsVizs?;
  onReloadData?;
  onAddToDashBoard?;
  onPublish?;
  onRecycleViz?;
  openMockData?;
  allowDownload?: boolean;
  allowShare?: boolean;
  allowManage?: boolean;
}> = memo(
  ({
    onShareLinkClick,
    onDownloadDataLinkClick,
    openMockData,
    onSaveAsVizs,
    onReloadData,
    onAddToDashBoard,
    onPublish,
    allowDownload,
    allowShare,
    allowManage,
    onRecycleViz,
  }) => {
    const t = useI18NPrefix(`viz.action`);
    const tg = useI18NPrefix(`global`);

    const confirmLabel = (
      label: string,
      onConfirm: (() => void) | undefined,
      title = t('common.confirm'),
    ) => (
      <Popconfirm
        placement="left"
        title={title}
        onConfirm={onConfirm}
        okText={t('common.ok')}
        cancelText={t('common.cancel')}
      >
        {label}
      </Popconfirm>
    );
    const menuItems: MenuProps['items'] = [
      ...(onReloadData
        ? [
            {
              key: 'reloadData',
              icon: <ReloadOutlined />,
              onClick: onReloadData,
              label: t('syncData'),
            },
            { key: 'reloadDataLine', type: 'divider' as const },
          ]
        : []),
      ...(allowManage && onSaveAsVizs
        ? [
            {
              key: 'saveAs',
              icon: <CopyFilled />,
              onClick: onSaveAsVizs,
              label: tg('button.saveAs'),
            },
          ]
        : []),
      ...(allowManage && onSaveAsVizs
        ? [
            {
              key: 'addToDash',
              icon: <FileAddOutlined />,
              onClick: () => onAddToDashBoard(true),
              label: t('addToDash'),
            },
            { key: 'addToDashLine', type: 'divider' as const },
          ]
        : []),
      ...(allowShare && onShareLinkClick
        ? [
            {
              key: 'shareLink',
              icon: <ShareAltOutlined />,
              onClick: onShareLinkClick,
              label: t('share.shareLink'),
            },
          ]
        : []),
      ...(allowDownload && onDownloadDataLinkClick
        ? [
            {
              key: 'exportData',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportData'), () =>
                onDownloadDataLinkClick(DownloadFileType.Excel),
              ),
            },
            {
              key: 'exportPDF',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportPDF'), () =>
                onDownloadDataLinkClick(DownloadFileType.Pdf),
              ),
            },
            {
              key: 'exportPicture',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportPicture'), () =>
                onDownloadDataLinkClick(DownloadFileType.Image),
              ),
            },
            {
              key: 'exportTpl',
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(t('share.exportTpl'), openMockData),
            },
            { type: 'divider' as const },
            { key: 'downloadDataLine', type: 'divider' as const },
          ]
        : []),
      ...(allowManage && onPublish
        ? [
            {
              key: 'publish',
              icon: <VerticalAlignBottomOutlined />,
              onClick: onPublish,
              label: t('unpublish'),
            },
          ]
        : []),
      ...(allowManage && onRecycleViz
        ? [
            {
              key: 'delete',
              icon: <DeleteOutlined />,
              label: confirmLabel(
                tg('button.archive'),
                onRecycleViz,
                tg('operation.archiveConfirm'),
              ),
            },
          ]
        : []),
    ];

    return (
      <StyleVizOperationMenu>
        <Menu items={menuItems} />
      </StyleVizOperationMenu>
    );
  },
);

export default VizOperationMenu;

const StyleVizOperationMenu = styled.div``;
