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

import { Popconfirm, PopconfirmProps } from 'antd';
import { MouseEvent, ReactNode, useCallback } from 'react';

export interface ConfirmMenuLabelProps {
  open: boolean;
  children: ReactNode;
  title: ReactNode;
  okText: ReactNode;
  cancelText: ReactNode;
  placement?: PopconfirmProps['placement'];
  onOpen: () => void;
  onConfirm?: () => void;
  onClose: () => void;
}

export function ConfirmMenuLabel({
  open,
  children,
  title,
  okText,
  cancelText,
  placement = 'left',
  onOpen,
  onConfirm,
  onClose,
}: ConfirmMenuLabelProps) {
  const handleTriggerClick = useCallback(
    (event: MouseEvent<HTMLElement>) => {
      event.preventDefault();
      event.stopPropagation();
      onOpen();
    },
    [onOpen],
  );

  const handleOpenChange = useCallback(
    (nextOpen: boolean) => {
      if (nextOpen) {
        onOpen();
      } else {
        onClose();
      }
    },
    [onClose, onOpen],
  );

  const handleConfirm = useCallback(() => {
    onConfirm?.();
    onClose();
  }, [onClose, onConfirm]);

  return (
    <Popconfirm
      placement={placement}
      title={title}
      open={open}
      okText={okText}
      cancelText={cancelText}
      onOpenChange={handleOpenChange}
      onConfirm={handleConfirm}
      onCancel={onClose}
      getPopupContainer={triggerNode =>
        triggerNode.closest('.ant-dropdown') || document.body
      }
    >
      <span onClick={handleTriggerClick}>{children}</span>
    </Popconfirm>
  );
}
