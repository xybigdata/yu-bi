import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';

import { ConfirmMenuLabel } from '../ConfirmMenuLabel';

describe('ConfirmMenuLabel', () => {
  test('should keep menu click from bubbling while opening confirm popover', () => {
    const onParentClick = vi.fn();
    const onOpen = vi.fn();

    render(
      <div onClick={onParentClick}>
        <ConfirmMenuLabel
          open={false}
          title="请确认"
          okText="确认"
          cancelText="取消"
          onOpen={onOpen}
          onClose={vi.fn()}
        >
          导出到 PDF
        </ConfirmMenuLabel>
      </div>,
    );

    fireEvent.click(screen.getByText('导出到 PDF'));

    expect(onOpen).toHaveBeenCalled();
    expect(onParentClick).not.toHaveBeenCalled();
  });

  test('should run confirm action before closing menu', () => {
    const onConfirm = vi.fn();
    const onClose = vi.fn();

    render(
      <ConfirmMenuLabel
        open
        title="请确认"
        okText="确认"
        cancelText="取消"
        onOpen={vi.fn()}
        onClose={onClose}
        onConfirm={onConfirm}
      >
        导出到 PDF
      </ConfirmMenuLabel>,
    );

    fireEvent.click(screen.getByRole('button', { name: '确 认' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onClose).toHaveBeenCalled();
    expect(onConfirm.mock.invocationCallOrder[0]).toBeLessThan(
      onClose.mock.invocationCallOrder[0],
    );
  });
});
