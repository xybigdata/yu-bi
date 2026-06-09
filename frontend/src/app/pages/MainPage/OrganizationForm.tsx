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

import { Form, Input, Modal, ModalProps } from 'antd';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import React, { useCallback } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { selectSaveOrganizationLoading } from './slice/selectors';
import { addOrganization } from './slice/thunks';

const FormItem = Form.Item;

interface OrganizationFormProps extends Omit<ModalProps, 'onCancel'> {
  onCancel: () => void;
}

export function OrganizationForm({ open, onCancel }: OrganizationFormProps) {
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const loading = useSelector(selectSaveOrganizationLoading);
  const [form] = Form.useForm();
  const t = useI18NPrefix('main.nav.organization');
  const tg = useI18NPrefix('global');

  const formSubmit = useCallback(
    values => {
      dispatch(
        addOrganization({
          organization: values,
          resolve: () => {
            onCancel();
            navigate.push('/');
          },
        }),
      );
    },
    [dispatch, navigate, onCancel],
  );

  const afterClose = useCallback(() => {
    form.resetFields();
  }, [form]);

  const save = useCallback(() => {
    form.submit();
  }, [form]);

  return (
    <Modal
      title={t('create')}
      open={open}
      okText={t('save')}
      confirmLoading={loading}
      onOk={save}
      onCancel={onCancel}
      afterClose={afterClose}
    >
      <Form
        form={form}
        labelCol={{ span: 6 }}
        labelAlign="left"
        wrapperCol={{ span: 16 }}
        onFinish={formSubmit}
      >
        <FormItem
          name="name"
          label={t('name')}
          getValueFromEvent={event => event.target.value?.trim()}
          rules={[
            {
              required: true,
              message: `${t('name')}${tg('validation.required')}`,
            },
            {
              validator: debounce((_, value) => {
                if (!value) {
                  return Promise.resolve();
                }
                if (!value.trim()) {
                  return Promise.reject(
                    `${t('name')}${tg('validation.required')}`,
                  );
                }
                const data = { name: value };
                return fetchCheckName('orgs', data);
              }, DEFAULT_DEBOUNCE_WAIT),
            },
          ]}
        >
          <Input />
        </FormItem>
        <FormItem name="description" label={t('desc')}>
          <Input.TextArea autoSize={{ minRows: 4, maxRows: 8 }} />
        </FormItem>
      </Form>
    </Modal>
  );
}
