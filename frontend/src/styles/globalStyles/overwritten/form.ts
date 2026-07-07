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
import { BORDER_RADIUS } from 'styles/StyleConstants';

export const Form = createGlobalStyle`
  body {
    .ant-form,
    .ant-form-css-var,
    .ant-form.ant-form-css-var,
    .ant-form[class*="css-var-"] {
      --ant-form-label-color: ${p => p.theme.textColorLight} !important;
    }

    .ant-form .ant-form-item .ant-form-item-label > label,
    .ant-form .ant-form-item .ant-form-item-row > .ant-col > label,
    .ant-form .ant-form-item .ant-form-item-row > .ant-form-item-label > label {
      color: ${p => p.theme.textColorLight} !important;
    }
  }

  .yubi-ant-input {
    &.ant-input {
      color: ${p => p.theme.textColorSnd};
      background-color: ${p => p.theme.bodyBackground};
      border-color: ${p => p.theme.bodyBackground};
      box-shadow: none;

      &:hover,
      &:focus {
        border-color: ${p => p.theme.bodyBackground};
        box-shadow: none;
      }

      &:focus {
        background-color: ${p => p.theme.emphasisBackground};
      }
    }
  }

  .yubi-ant-select {
    &.ant-select {
      color: ${p => p.theme.textColorSnd};
    }

    &.ant-select:not(.ant-select-customize-input) .ant-select-selector {
      background-color: ${p => p.theme.bodyBackground};
      border-color: ${p => p.theme.bodyBackground} !important;
      border-radius: ${BORDER_RADIUS};
      box-shadow: none !important;
    }
  }

  .yubi-ant-input-number {
    &.ant-input-number {
      width: 100%;
      background-color: ${p => p.theme.bodyBackground};
      border-color: ${p => p.theme.bodyBackground};
      border-radius: ${BORDER_RADIUS};
      box-shadow: none;

      &:hover,
      &:focus {
        border-color: ${p => p.theme.bodyBackground};
        box-shadow: none;
      }

      &:focus {
        background-color: ${p => p.theme.bodyBackground};
      }
    }

    .ant-input-number-input {
      color: ${p => p.theme.textColorSnd};
    }

    .ant-input-number-handler-wrap {
      background-color: ${p => p.theme.bodyBackground};
    }

    .ant-input-number-disabled {
      background-color: ${p => p.theme.textColorDisabled};
    }
  }

  .yubi-ant-upload {
    &.ant-upload.ant-upload-drag {
      background-color: ${p => p.theme.bodyBackground} !important;
      border-color: transparent !important;
      border-radius: ${BORDER_RADIUS};
    }
  }

  .yubi-modal-button {
    &.ant-btn {
      color: ${p => p.theme.textColorSnd};
      background-color: ${p => p.theme.bodyBackground};
      border: 0;
      border-radius: ${BORDER_RADIUS};

      &:hover,
      &:active {
        color: ${p => p.theme.textColorSnd};
        background-color: ${p => p.theme.emphasisBackground};
      }
    }
  }
`;
