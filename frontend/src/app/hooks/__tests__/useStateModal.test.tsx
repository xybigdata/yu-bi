import { renderHook } from '@testing-library/react';
import type { CSSProperties } from 'react';
import { describe, expect, test, vi } from 'vitest';
import useStateModal from '../useStateModal';

const modalMock = vi.hoisted(() => ({
  latestConfirmProps: undefined as
    | {
        bodyStyle?: CSSProperties;
        className?: string;
        onCancel?: (close?: () => void) => void;
        styles?: {
          body?: CSSProperties;
        };
      }
    | undefined,
  resetFields: vi.fn(),
  useForm: vi.fn(() => [{ resetFields: modalMock.resetFields }]),
  confirm: vi.fn(props => {
    modalMock.latestConfirmProps = props;
    return {};
  }),
  destroyAll: vi.fn(),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    Form: {
      ...actual.Form,
      useForm: modalMock.useForm,
    },
    Modal: {
      ...actual.Modal,
      useModal: () => [{ confirm: modalMock.confirm }, null],
      destroyAll: modalMock.destroyAll,
    },
  };
});

describe('useStateModal', () => {
  test('should render state modals with shared Datart-aligned modal chrome', () => {
    const { result } = renderHook(() => useStateModal({}));

    result.current[0]({
      content: null,
    });

    expect(modalMock.latestConfirmProps?.className).toBe('yubi-state-modal');
    expect(modalMock.latestConfirmProps?.bodyStyle).toMatchObject({
      maxHeight: 'calc(100vh - 48px)',
      padding: '32px 32px 24px',
    });
    expect(modalMock.latestConfirmProps?.styles?.body).toMatchObject({
      maxHeight: 'calc(100vh - 48px)',
      padding: '32px 32px 24px',
    });
  });

  test('should preserve custom state modal body overflow while keeping shared padding', () => {
    const { result } = renderHook(() => useStateModal({}));

    result.current[0]({
      bodyStyle: {
        maxHeight: 760,
        overflowY: 'auto',
        overflowX: 'hidden',
      },
      content: null,
    });

    expect(modalMock.latestConfirmProps?.styles?.body).toMatchObject({
      maxHeight: 760,
      overflowY: 'auto',
      overflowX: 'hidden',
      padding: '32px 32px 24px',
    });
  });

  test('should close modal when cancel callback does not call close', () => {
    const onCancel = vi.fn();
    const close = vi.fn();
    const { result } = renderHook(() => useStateModal({}));

    result.current[0]({
      content: null,
      onCancel,
    });

    modalMock.latestConfirmProps?.onCancel?.(close);

    expect(onCancel).toHaveBeenCalledWith(expect.any(Function));
    expect(close).toHaveBeenCalledTimes(1);
    expect(modalMock.resetFields).toHaveBeenCalled();
  });

  test('should close modal only once when cancel callback calls close', () => {
    const close = vi.fn();
    const { result } = renderHook(() => useStateModal({}));

    result.current[0]({
      content: null,
      onCancel: closeFn => closeFn?.(),
    });

    modalMock.latestConfirmProps?.onCancel?.(close);

    expect(close).toHaveBeenCalledTimes(1);
  });
});
