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

import { UploadOutlined } from '@ant-design/icons';
import { Button, Form, FormInstance, Input, message, Upload } from 'antd';
import type { UploadProps } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BASE_API_URL } from 'globalConstants';
import { useCallback, useState } from 'react';
import styled from 'styled-components';
import { APIResponse } from 'types';
import { getToken } from 'utils/auth';

interface FileUploadProps {
  form?: FormInstance;
  sourceId?: string;
  loading?: boolean;
  dataTables?: table[];
  onTest?: () => void;
}

interface table {
  tableName?: string;
}

export const getDatasourceFileUploadError = (
  response?: Partial<APIResponse<string>>,
) => response?.message || response?.exception;

export const isDatasourceFileUploading = (status?: string) =>
  status === 'uploading';

export const isDatasourceFileUploadSettled = (status?: string) =>
  status === 'done' || status === 'error' || status === 'removed';

export const getDatasourceFileUploadAction = (sourceId?: string) =>
  `${BASE_API_URL}/files/datasource?sourceId=${sourceId || ''}`;

export function FileUpload({
  form,
  sourceId,
  loading,
  dataTables,
  onTest,
}: FileUploadProps) {
  const [uploadFileLoading, setUploadFileLoading] = useState(false);
  const t = useI18NPrefix('source');
  const tg = useI18NPrefix('global');

  const normFile = useCallback(e => {
    if (Array.isArray(e)) {
      return e;
    }
    return e && e.fileList;
  }, []);

  const getUniqueName = useCallback(
    (name: string, names: (string | undefined)[]) => {
      if (names.includes(name)) {
        return getUniqueName(name + '_' + tg('copy'), names);
      }
      return name;
    },
    [tg],
  );

  const beforeUpload: UploadProps['beforeUpload'] = useCallback(
    file => {
      if (!sourceId) {
        message.error(t('form.saveSourceBeforeUpload'));
        setUploadFileLoading(false);
        return Upload.LIST_IGNORE;
      }

      const tableName = form?.getFieldValue('config')?.tableName;
      if (tableName) {
        return true;
      }

      const fileName = file.name.substring(0, file.name.lastIndexOf('.'));
      let tableNames = (dataTables || []).map(table => table.tableName);
      let uniqueTableName = getUniqueName(fileName, tableNames);
      form?.setFieldsValue({
        config: {
          tableName: uniqueTableName,
        },
      });
      return true;
    },
    [dataTables, form, getUniqueName, sourceId, t],
  );

  const uploadChange: UploadProps['onChange'] = useCallback(
    async ({ file }) => {
      if (isDatasourceFileUploading(file.status)) {
        setUploadFileLoading(true);
        return;
      }

      if (file.status === 'done') {
        setUploadFileLoading(false);
        const format = file.name
          .substr(file.name.lastIndexOf('.') + 1)
          .toUpperCase();
        const response = file.response as APIResponse<string> | undefined;
        if (response?.success) {
          form &&
            form.setFieldsValue({
              config: {
                path: response.data,
                format,
              },
            });
          onTest && onTest();
        } else {
          message.error(
            getDatasourceFileUploadError(response) || t('form.uploadFailed'),
          );
        }
        return;
      }

      if (file.status === 'error') {
        setUploadFileLoading(false);
        message.error(t('form.uploadFailed'));
        return;
      }

      if (isDatasourceFileUploadSettled(file.status)) {
        setUploadFileLoading(false);
      }
    },
    [form, onTest, t],
  );

  return (
    <>
      <Form.Item
        label={t('form.file')}
        valuePropName="fileList"
        getValueFromEvent={normFile}
      >
        <Upload
          accept=".xlsx,.xls,.csv"
          method="post"
          action={getDatasourceFileUploadAction(sourceId)}
          headers={{ authorization: getToken()! }}
          showUploadList={false}
          beforeUpload={beforeUpload}
          onChange={uploadChange}
          disabled={loading}
        >
          <Button
            icon={<UploadOutlined />}
            loading={uploadFileLoading || loading}
            disabled={loading}
          >
            {t('form.selectFile')}
          </Button>
        </Upload>
      </Form.Item>
      <HiddenFormItem name={['config', 'path']}>
        <Input />
      </HiddenFormItem>
      <HiddenFormItem name={['config', 'format']}>
        <Input />
      </HiddenFormItem>
    </>
  );
}

const HiddenFormItem = styled(Form.Item)`
  display: none;
`;
