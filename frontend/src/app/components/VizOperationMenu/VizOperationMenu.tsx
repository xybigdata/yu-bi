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
  VerticalAlignBottomOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps } from 'antd';
import { DownloadFileType } from 'app/constants';
import { ConfirmMenuLabel } from 'app/components/Popup';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, memo, useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';

interface VizOperationMenuProps {
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
  onCloseDropdown?: () => void;
}

export const useVizOperationMenuItems = ({
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
  onCloseDropdown,
}: VizOperationMenuProps) => {
  const t = useI18NPrefix(`viz.action`);
  const tg = useI18NPrefix(`global`);
  const [confirmKey, setConfirmKey] = useState<string>();
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
    const confirmLabel = (
      key: string,
      label: string,
      onConfirm: (() => void) | undefined,
      title = t('common.confirm'),
    ) => (
      <ConfirmMenuLabel
        open={confirmKey === key}
        placement="left"
        title={title}
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
              onClick: openConfirmMenuFromItem('exportData'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel('exportData', t('share.exportData'), () =>
                onDownloadDataLinkClick(DownloadFileType.Excel),
              ),
            },
            {
              key: 'exportPDF',
              onClick: openConfirmMenuFromItem('exportPDF'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel('exportPDF', t('share.exportPDF'), () =>
                onDownloadDataLinkClick(DownloadFileType.Pdf),
              ),
            },
            {
              key: 'exportPicture',
              onClick: openConfirmMenuFromItem('exportPicture'),
              icon: <CloudDownloadOutlined />,
              label: confirmLabel(
                'exportPicture',
                t('share.exportPicture'),
                () => onDownloadDataLinkClick(DownloadFileType.Image),
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
              onClick: openConfirmMenuFromItem('delete'),
              label: confirmLabel(
                'delete',
                tg('button.archive'),
                onRecycleViz,
                tg('operation.archiveConfirm'),
              ),
            },
          ]
        : []),
    ];
  }, [
    allowDownload,
    allowManage,
    allowShare,
    closeConfirmMenu,
    confirmKey,
    onAddToDashBoard,
    onDownloadDataLinkClick,
    openConfirmMenu,
    openConfirmMenuFromItem,
    onPublish,
    onRecycleViz,
    onReloadData,
    onSaveAsVizs,
    onShareLinkClick,
    openMockData,
    t,
    tg,
  ]);
};

const VizOperationMenu: FC<VizOperationMenuProps> = memo(({ ...props }) => {
  const menuItems = useVizOperationMenuItems(props);

  return (
    <StyleVizOperationMenu>
      <Menu items={menuItems} />
    </StyleVizOperationMenu>
  );
});

export default VizOperationMenu;

const StyleVizOperationMenu = styled.div``;
