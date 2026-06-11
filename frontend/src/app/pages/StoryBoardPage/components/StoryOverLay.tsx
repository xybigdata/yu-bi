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
  DeleteOutlined,
  ShareAltOutlined,
  VerticalAlignBottomOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps, Popconfirm } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useRecycleViz } from 'app/hooks/useRecycleViz';
import { memo, useContext, useMemo } from 'react';
import { StoryContext } from '../contexts/StoryContext';

export interface BoardOverLayProps {
  onOpenShareLink?: () => void;
  onBoardToDownLoad?: () => void;
  onShareDownloadData?: () => void;
  onPublish?;
  allowShare?: boolean;
  allowManage?: boolean;
}
export const useStoryOverlayItems = ({
  onOpenShareLink,
  allowShare,
  allowManage,
  onPublish,
}: BoardOverLayProps) => {
  const t = useI18NPrefix(`viz.action`);
  const tg = useI18NPrefix(`global`);
  const { storyId: stroyId, orgId } = useContext(StoryContext);
  const recycleViz = useRecycleViz(orgId, stroyId, 'STORYBOARD');

  return useMemo<MenuProps['items']>(() => {
    const renderList = [
      {
        key: 'shareLink',
        icon: <ShareAltOutlined />,
        onClick: onOpenShareLink,
        disabled: false,
        render: allowShare,
        content: t('share.shareLink'),
        className: 'line',
      },
      {
        key: 'publish',
        icon: <VerticalAlignBottomOutlined />,
        onClick: onPublish,
        disabled: false,
        render: allowManage && onPublish,
        content: t('unpublish'),
      },
      {
        key: 'delete',
        icon: <DeleteOutlined />,
        disabled: false,
        render: allowManage,
        content: (
          <Popconfirm
            title={tg('operation.archiveConfirm')}
            onConfirm={recycleViz}
          >
            {tg('button.archive')}
          </Popconfirm>
        ),
      },
    ];

    return renderList
      .filter(item => item.render)
      .flatMap<NonNullable<MenuProps['items']>[number]>(item => {
        const menuItem: NonNullable<MenuProps['items']>[number] = {
          key: item.key,
          icon: item.icon,
          label: item.content,
          onClick: item.onClick,
        };
        return item.className
          ? [menuItem, { key: `${item.key}Line`, type: 'divider' }]
          : [menuItem];
      });
  }, [allowManage, allowShare, onOpenShareLink, onPublish, recycleViz, t, tg]);
};

export const StoryOverLay: React.FC<BoardOverLayProps> = memo(props => {
  const actionItems = useStoryOverlayItems(props);
  return <Menu items={actionItems} />;
});
