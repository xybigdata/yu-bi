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
  BgColorsOutlined,
  DiffOutlined,
  DownOutlined,
  FilterOutlined,
  FontSizeOutlined,
  FormatPainterOutlined,
  GroupOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
} from '@ant-design/icons';
import { Dropdown } from 'antd';
import { StateModalSize } from 'app/hooks/useStateModal';
import { SortActionType } from 'app/constants';
import ChartDataViewContext from 'app/pages/ChartWorkbenchPage/contexts/ChartDataViewContext';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataConfigSectionProps } from 'app/types/ChartDataConfigSection';
import { getColumnRenderName } from 'app/utils/chartHelper';
import classnames from 'classnames';
import { FC, ReactNode, memo, useContext, useMemo } from 'react';
import styled, { keyframes } from 'styled-components';
import { ERROR, WARNING, WHITE } from 'styles/StyleConstants';
import {
  findSameFieldInView,
  getAllFieldsOfEachType,
  getCanReplaceViewFields,
} from '../../utils';
import ChartDataConfigSectionActionMenu from './ChartDataConfigSectionActionMenu';
import { ChartDataConfigSectionReplaceMenu } from './ChartDataConfigSectionReplaceMenu';

type DropdownPopupRenderCompatProps = {
  popupRender?: (originNode: React.ReactNode) => React.ReactNode;
};

type DropdownDestroyOnHiddenCompatProps = {
  destroyOnHidden?: boolean;
};

const ChartDraggableElementField: FC<{
  modalSize?: StateModalSize;
  config: ChartDataConfig;
  columnConfig: ChartDataSectionField;
  ancestors: number[];
  aggregation?: boolean;
  availableSourceFunctions?: string[];
  onConfigChanged: ChartDataConfigSectionProps['onConfigChanged'];
  handleOpenActionModal: (uid: string) => (actionType: string) => void;
}> = memo(
  ({
    modalSize,
    config,
    columnConfig,
    ancestors,
    aggregation,
    availableSourceFunctions,
    onConfigChanged,
    handleOpenActionModal,
  }) => {
    const { dataView } = useContext(ChartDataViewContext);

    const canReplaceViewFields = useMemo(() => {
      const {
        hierarchyFields,
        stringFields,
        dateLevelFields,
        numericFields,
        stringComputedFields,
        numericComputedFields,
        dateComputedFields,
      } = getAllFieldsOfEachType({
        sortType: 'byNameSort',
        dataView,
        availableSourceFunctions,
      });
      const viewFields = [
        ...hierarchyFields,
        ...stringFields,
        ...dateLevelFields,
        ...numericFields,
        ...stringComputedFields,
        ...numericComputedFields,
        ...dateComputedFields,
      ];
      return getCanReplaceViewFields(viewFields, columnConfig);
    }, [availableSourceFunctions, columnConfig, dataView]);
    const showReplaceMenu = useMemo(() => {
      return !findSameFieldInView(canReplaceViewFields, columnConfig);
    }, [canReplaceViewFields, columnConfig]);

    const renderActionExtensionMenu = () => {
      if (showReplaceMenu) {
        return (
          <ChartDataConfigSectionReplaceMenu
            uid={columnConfig.uid!}
            viewFields={canReplaceViewFields}
            type={columnConfig.type}
            columnConfig={columnConfig}
            ancestors={ancestors}
            config={config}
            onConfigChanged={onConfigChanged}
          />
        );
      }

      return (
        <ChartDataConfigSectionActionMenu
          uid={columnConfig.uid!}
          type={columnConfig.type}
          category={columnConfig.category}
          ancestors={ancestors}
          config={config}
          modalSize={modalSize}
          availableSourceFunctions={availableSourceFunctions}
          metas={dataView?.meta}
          onConfigChanged={onConfigChanged}
          onOpenModal={handleOpenActionModal}
        />
      );
    };
    const dropdownPopupRenderProps = {
      popupRender: () => renderActionExtensionMenu(),
    } as DropdownPopupRenderCompatProps;
    const dropdownDestroyOnHiddenProps = {
      destroyOnHidden: true,
    } as DropdownDestroyOnHiddenCompatProps;

    const enableActionsIcons = (col: ChartDataSectionField) => {
      const icons: ReactNode[] = [];
      if (col.alias) {
        icons.push(<DiffOutlined key="alias" />);
      }
      if (col.sort) {
        if (col.sort.type === SortActionType.ASC) {
          icons.push(<SortAscendingOutlined key="sort" />);
        }
        if (col.sort.type === SortActionType.DESC) {
          icons.push(<SortDescendingOutlined key="sort" />);
        }
      }
      if (col.format) {
        icons.push(<FormatPainterOutlined key="format" />);
      }
      if (col.aggregate) {
        icons.push(<GroupOutlined key="aggregate" />);
      }
      if (col.filter) {
        icons.push(<FilterOutlined key="filter" />);
      }
      if (col.color) {
        icons.push(<BgColorsOutlined key="color" />);
      }
      if (col.size) {
        icons.push(<FontSizeOutlined key="size" />);
      }
      return icons;
    };

    return (
      <Dropdown
        key={columnConfig.uid}
        disabled={!config?.actions}
        {...dropdownDestroyOnHiddenProps}
        {...dropdownPopupRenderProps}
        overlayClassName="datart-data-section-dropdown"
        trigger={['click']}
      >
        <StyledWrapper className={classnames({ replace: showReplaceMenu })}>
          {config?.actions && <DownOutlined style={{ marginRight: '10px' }} />}
          <span>
            {aggregation === false
              ? columnConfig.colName
              : getColumnRenderName(columnConfig)}
          </span>
          <div style={{ display: 'inline-block', marginLeft: '5px' }}>
            {enableActionsIcons(columnConfig)}
          </div>
        </StyledWrapper>
      </Dropdown>
    );
  },
);

export default ChartDraggableElementField;

const warningColor = keyframes`
  0% { color: ${ERROR}; }
  25% { color: ${WARNING}; }
  50% { color: ${WHITE};}
  100% { color: ${ERROR};}
`;
const StyledWrapper = styled.div`
  &.replace {
    animation: ${warningColor} 2s linear infinite;
  }
`;
