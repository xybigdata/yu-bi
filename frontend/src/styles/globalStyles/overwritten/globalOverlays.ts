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

import { createGlobalStyle } from 'styled-components';
import {
  LEVEL_1000,
  SPACE,
  SPACE_SM,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';

const DATART_MODAL_SHADOW =
  '0 3px 6px -4px rgba(0, 0, 0, 0.12), ' +
  '0 6px 16px 0 rgba(0, 0, 0, 0.08), ' +
  '0 9px 28px 8px rgba(0, 0, 0, 0.05)';

export const GlobalOverlays = createGlobalStyle`
  /* AntD 6 下拉子菜单箭头已有 SVG，旧伪元素会在右侧留下额外黑色短划。 */
  .ant-dropdown-menu-submenu-arrow::before,
  .ant-dropdown-menu-submenu-arrow::after {
    display: none !important;
    content: none !important;
  }

  /* shared modal chrome aligned with Datart */
  .ant-modal.yubi-plain-modal,
  .ant-modal.yubi-form-modal,
  .ant-modal.yubi-state-modal {
    padding-bottom: ${SPACE_TIMES(6)} !important;
    color: ${p => p.theme.textColor};
  }

  .ant-modal.yubi-plain-modal .ant-modal-content,
  .ant-modal.yubi-form-modal .ant-modal-content,
  .ant-modal.yubi-state-modal .ant-modal-content,
  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-container {
    padding: 0;
    overflow: visible;
    background: ${p => p.theme.componentBackground};
    border-radius: 2px;
    box-shadow: ${DATART_MODAL_SHADOW};
  }

  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-container {
    width: 100%;
  }

  .ant-modal.yubi-plain-modal .ant-modal-body,
  .ant-modal.yubi-form-modal .ant-modal-body,
  .ant-modal.yubi-state-modal .ant-modal-body {
    padding: ${SPACE_TIMES(8)} ${SPACE_TIMES(8)} ${SPACE_TIMES(6)};
    color: ${p => p.theme.textColor};
  }

  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-confirm-body-wrapper,
  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-confirm-body,
  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-confirm-paragraph,
  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-confirm-content {
    width: 100%;
    max-width: none;
    margin-inline-start: 0;
  }

  .ant-modal.yubi-state-modal.ant-modal-confirm .ant-modal-confirm-btns {
    margin-top: ${SPACE_TIMES(6)};
  }

  /* app/components/Popup */
  .yubi-popup {
    z-index: ${LEVEL_1000 - 1};

    &.on-modal {
      z-index: ${LEVEL_1000 + 30};
    }

    .ant-popover-arrow {
      display: none;
    }
    .ant-popover-content {
      padding: 0;
    }
    .ant-dropdown-menu {
      border-inline-end: 0 !important;
      border-right: 0 !important;
      box-shadow: none;
    }
    &.ant-popover-placement-bottom,
    &.ant-popover-placement-bottomLeft,
    &.ant-popover-placement-bottomRight {
      padding-top: 0;
    }
  }
  

  /* schema table header action dropdown menu */
  .yubi-schema-table-header-menu {
    min-width: ${SPACE_TIMES(40)};

    .ant-dropdown-menu {
      border-inline-end: 0 !important;
      border-right: 0 !important;
    }

    .ant-dropdown-menu-submenu-selected {
      .ant-dropdown-menu-submenu-title {
        color: ${p => p.theme.textColor};
      }
    }
  }

  /* config panel */
  .yubi-config-panel {
    &.ant-collapse >
    .ant-collapse-item >
    .ant-collapse-header {
      padding: ${SPACE_XS} 0;
      color: ${p => p.theme.textColor};

      .ant-collapse-arrow {
        margin-right: ${SPACE_XS};
      }
    }

    .ant-collapse-content >
    .ant-collapse-content-box {
      padding: ${SPACE_XS} 0 ${SPACE_SM} !important;
    }
  }

  /* data config section dropdown */
  .yubi-data-section-dropdown {
    z-index: ${LEVEL_1000 - 1};

    .ant-dropdown-menu {
      border-inline-end: 0 !important;
      border-right: 0 !important;
    }
  }

  /* chart workbench data view selector */
  .yubi-chart-dataview-selector-popup {
    .ant-select-tree {
      .ant-select-tree-treenode {
        align-items: center;
      }

      .ant-select-tree-indent-unit {
        width: ${SPACE_TIMES(4)};
      }

      .ant-select-tree-switcher {
        width: ${SPACE_TIMES(4)};
        margin-right: ${SPACE};
      }

      .ant-select-tree-switcher-noop {
        width: 0;
        min-width: 0;
        margin-right: ${SPACE};
      }

      .ant-select-tree-node-content-wrapper {
        padding-inline: ${SPACE} ${SPACE_XS};
      }
    }
  }

  /* view column permissions field picker */
  .yubi-column-permission-tree-popup {
    .ant-popover-inner {
      padding: 0;
    }

    .column-permission-tree {
      .ant-tree-treenode {
        display: flex;
        align-items: center;
        min-height: ${SPACE_TIMES(7)};
        padding-top: 0;
        padding-bottom: 0;
      }

      .ant-tree-checkbox {
        top: 0;
        display: inline-flex;
        align-items: center;
        align-self: center;
        justify-content: center;
        margin: 0 ${SPACE_XS} 0 0;
      }

      .ant-tree-node-content-wrapper {
        display: flex;
        align-items: center;
        min-height: ${SPACE_TIMES(7)};
        line-height: ${SPACE_TIMES(7)};
      }

      .ant-tree-title {
        display: flex;
        align-items: center;
        height: ${SPACE_TIMES(7)};
        padding-left: ${SPACE};
      }
    }
  }

  /* data model computed field edit/delete menu */
  .yubi-data-model-computed-field-menu-popup {
    .ant-popover-container,
    .ant-popover-inner {
      box-sizing: border-box;
      width: 95px;
      padding: 10px 0;
      overflow: hidden;
      border: 0;
    }

    .ant-popover-content,
    .ant-popover-inner-content {
      width: 100%;
      padding: 0;
    }

    .ant-dropdown-menu {
      width: 95px;
      min-width: 95px;
      padding: 0;
      overflow: hidden;
      border-inline-end: 0 !important;
      border-right: 0 !important;
    }

    .ant-dropdown-menu-item {
      min-height: 45px;
      padding: 0;
      margin: 0;
    }

    .ant-dropdown-menu-title-content {
      width: 100%;
    }

    .data-model-computed-field-menu-item {
      box-sizing: border-box;
      width: 100%;
      height: 45px;
      padding: 0 ${SPACE_SM};
      overflow: hidden;
      line-height: 1;
    }

    .data-model-computed-field-menu-item > .prefix {
      margin-right: ${SPACE_XS};
    }
  }

  /* sidebar tree node action menus */
  .yubi-tree-more-menu-popup {
    .ant-popover-container,
    .ant-popover-inner {
      box-sizing: border-box;
      min-width: ${SPACE_TIMES(30)};
      padding: ${SPACE} 0;
      overflow: hidden;
      border: 0;
    }

    .ant-popover-content,
    .ant-popover-inner-content {
      width: 100%;
      padding: 0;
    }

    .ant-dropdown-menu {
      min-width: ${SPACE_TIMES(30)};
      padding: 0;
      overflow: hidden;
      border-inline-end: 0 !important;
      border-right: 0 !important;
    }

    .ant-dropdown-menu-item {
      min-height: ${SPACE_TIMES(9)};
      padding: 0;
      margin: 0;
    }

    .ant-dropdown-menu-title-content {
      width: 100%;
    }

    .yubi-tree-more-menu-item {
      box-sizing: border-box;
      min-width: ${SPACE_TIMES(30)};
      height: ${SPACE_TIMES(9)};
      padding: 0 ${SPACE_SM};
      overflow: hidden;
      line-height: 1;
    }

    .yubi-tree-more-menu-item > .prefix {
      margin-right: ${SPACE_XS};
    }
  }

  /* sidebar title recycle/collapse menus */
  .yubi-sidebar-title-more-menu-popup {
    .ant-popover-container,
    .ant-popover-inner {
      box-sizing: border-box;
      width: 105px;
      height: 110px;
      padding: 1px 0;
      overflow: hidden;
      border: 0;
    }

    .ant-popover-content,
    .ant-popover-inner-content {
      width: 100%;
      height: 100%;
      padding: 0;
    }

    .ant-dropdown-menu {
      width: 105px;
      min-width: 105px;
      padding: 0;
      overflow: hidden;
      border-inline-end: 0 !important;
      border-right: 0 !important;
    }

    .ant-dropdown-menu-item {
      min-height: 54px;
      padding: 0;
      margin: 0;
    }

    .ant-dropdown-menu-title-content {
      width: 100%;
    }

    .sidebar-title-more-menu-item {
      box-sizing: border-box;
      width: 100%;
      height: 54px;
      padding: 0 ${SPACE_SM};
      overflow: hidden;
      line-height: 1;
    }

    .sidebar-title-more-menu-item > .prefix {
      margin-right: ${SPACE_XS};
    }
  }

  /* color popover */
  .yubi-aggregation-colorpopover{
    .ant-popover-arrow{
      display:none;
    }
  }
`;
