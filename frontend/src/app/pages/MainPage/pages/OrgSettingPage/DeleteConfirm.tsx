import { Button, Form, Input, message, Modal, ModalProps } from 'antd';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectCurrentOrganization,
  selectDeleteOrganizationLoading,
} from 'app/pages/MainPage/slice/selectors';
import { useCallback, useState } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { deleteOrganization } from '../../slice/thunks';

type DeleteConfirmProps = Omit<ModalProps, 'visible'>;

export const DeleteConfirm = ({ open, ...props }: DeleteConfirmProps) => {
  const [inputValue, setInputValue] = useState('');
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const currentOrganization = useSelector(selectCurrentOrganization);
  const loading = useSelector(selectDeleteOrganizationLoading);
  const confirmDisabled = inputValue !== currentOrganization?.name;
  const t = useI18NPrefix('orgSetting');
  const tg = useI18NPrefix('global');

  const inputChange = useCallback(e => {
    setInputValue(e.target.value);
  }, []);

  const deleteOrg = useCallback(() => {
    dispatch(
      deleteOrganization(() => {
        navigate.replace('/');
        message.success(tg('operation.deleteSuccess'));
      }),
    );
  }, [dispatch, navigate, tg]);

  return (
    <Modal
      {...props}
      open={open}
      footer={[
        <Button key="cancel" onClick={props.onCancel}>
          {t('cancel')}
        </Button>,
        <Button
          key="confirm"
          loading={loading}
          disabled={confirmDisabled}
          onClick={deleteOrg}
          danger
        >
          {t('delete')}
        </Button>,
      ]}
    >
      <Form.Item>
        <Input
          placeholder={t('enterOrgName')}
          value={inputValue}
          onChange={inputChange}
        />
      </Form.Item>
    </Modal>
  );
};
