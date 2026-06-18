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

import { Form, FormInstance, Modal, ModalFuncProps } from 'antd';
import { ReactNode, useRef } from 'react';
import { isPromise } from 'utils/object';

export type StateModalCacheOnChange = (...args: unknown[]) => void;

export interface IStateModalContentProps {
  onChange: StateModalCacheOnChange;
}

export enum StateModalSize {
  XSMALL = 520,
  SMALL = 600,
  MIDDLE = 1000,
  LARGE = 1600,
  XLARGE = 2000,
}

const defaultBodyStyle: React.CSSProperties = {
  maxHeight: 1000,
  overflowY: 'auto',
  overflowX: 'auto',
};

type StateModalOnOk = (...args: never[]) => unknown;
type StateModalOnCancel = (close?: (() => void) | null) => void;
type StateModalContent =
  | ReactNode
  | ((
      cacheOnChangeValue: StateModalCacheOnChange,
      form?: FormInstance,
    ) => ReactNode);

type StateModalProps = {
  title?: ReactNode;
  content: StateModalContent;
  bodyStyle?: React.CSSProperties;
  modalSize?: string | number | StateModalSize;
  onOk?: StateModalOnOk;
  onCancel?: StateModalOnCancel;
  okButtonProps?: ModalFuncProps['okButtonProps'];
  cancelButtonProps?: ModalFuncProps['cancelButtonProps'];
  maskClosable?: boolean;
  centered?: boolean;
};

function useStateModal({ initState }: { initState?: unknown }) {
  const [form] = Form.useForm();
  const [modal, contextHolder] = Modal.useModal();
  const okCallbackRef = useRef<StateModalOnOk>();
  const cancelCallbackRef = useRef<StateModalOnCancel>();
  const stateRef = useRef<unknown[]>(initState ? [initState] : []);

  const handleSaveCacheValue: StateModalCacheOnChange = (...args) => {
    stateRef.current = args || [];
  };

  const handleClickOKButton = (closeFn?: () => void) => {
    return form
      .validateFields()
      .then(() => {
        try {
          const okCBResult = okCallbackRef.current?.call(
            Object.create(null),
            ...(stateRef.current as Parameters<StateModalOnOk>),
          );
          if (isPromise(okCBResult)) return okCBResult;
        } catch (e) {
          console.error('useStateModal | exception message ---> ', e);
        } finally {
          stateRef.current = [];
        }
        return closeFn;
      })
      .catch(info => {
        return Promise.reject(info);
      });
  };

  const handleClickCancelButton = (closeFn?: () => void) => {
    stateRef.current = [];
    cancelCallbackRef.current?.call(Object.create(null), closeFn || null);
  };

  const FormWrapper = (content: ReactNode) => {
    return (
      <Form form={form} name="state_modal_form">
        {content}
      </Form>
    );
  };

  const getModalSize = (size?: string | number | StateModalSize): number => {
    if (!size) {
      return StateModalSize.MIDDLE;
    }
    if (!isNaN(+size)) {
      return +size;
    }
    if (typeof size === 'string' && StateModalSize[size.toUpperCase()]) {
      return StateModalSize[size.toUpperCase()];
    }
    return StateModalSize.MIDDLE;
  };

  const renderContent = (content: StateModalContent) => {
    if (typeof content === 'function') {
      return content.call(Object.create(null), handleSaveCacheValue, form);
    }
    return content;
  };

  const showModal = (props: StateModalProps) => {
    okCallbackRef.current = props.onOk;
    cancelCallbackRef.current = props.onCancel;

    // Note: should destroy old modal and form effects in order to render new content
    Modal.destroyAll();
    form?.resetFields();

    return modal.confirm({
      title: props.title,
      width: getModalSize(props?.modalSize),
      bodyStyle: props.bodyStyle || defaultBodyStyle,
      content: FormWrapper(renderContent(props.content)),
      onOk: handleClickOKButton,
      onCancel: handleClickCancelButton,
      maskClosable: props.maskClosable ?? true,
      icon: null,
      centered: props.centered ?? true,
      okButtonProps: props.okButtonProps,
      cancelButtonProps: props.cancelButtonProps,
    });
  };

  return [showModal, contextHolder] as const;
}

export default useStateModal;
