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
import { css } from 'styled-components';

export const controllerSelectStyles = css`
  &.ant-select {
    display: flex;
    align-items: center;
    height: 32px;
    padding: 0 11px;
    line-height: 32px;
  }

  &.ant-select .ant-select-content {
    display: flex;
    flex: 1 1 auto;
    align-items: center;
    min-width: 0;
    height: 32px;
    margin-right: 6px;
    line-height: 32px;
  }

  &.ant-select-multiple .ant-select-content {
    flex-wrap: nowrap;
    overflow: hidden;
  }

  &.ant-select .ant-select-content-item {
    display: flex;
    align-items: center;
    min-height: 22px;
  }

  &.ant-select .ant-select-input,
  &.ant-select .ant-select-placeholder {
    line-height: 32px;
  }

  &.ant-select .ant-select-suffix {
    display: inline-flex;
    flex: 0 0 auto;
    align-items: center;
    justify-content: center;
    width: 12px;
    height: 32px;
    line-height: 1;
  }

  &.ant-select .ant-select-selector {
    background-color: transparent !important;
  }
`;
