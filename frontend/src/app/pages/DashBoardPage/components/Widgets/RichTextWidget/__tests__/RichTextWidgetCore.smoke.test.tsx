import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import {
  Quill,
  type DeltaStatic,
  type QuillInstance,
} from 'app/components/ChartGraph/BasicRichText/quillCompat';
import type { WidgetInfo } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import type { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { RichTextWidgetCore } from '../RichTextWidgetCore';

const { dispatchMock, clearActiveWidgetsMock } = vi.hoisted(() => ({
  dispatchMock: vi.fn(),
  clearActiveWidgetsMock: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: () => dispatchMock,
}));

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

vi.mock(
  'app/pages/DashBoardPage/components/ActionProvider/WidgetActionProvider',
  async importOriginal => {
    const React = await import('react');
    const actual =
      await importOriginal<
        typeof import('app/pages/DashBoardPage/components/ActionProvider/WidgetActionProvider')
      >();

    return {
      ...actual,
      WidgetActionContext: React.createContext({
        onEditClearActiveWidgets: clearActiveWidgetsMock,
      }),
    };
  },
);

function createWidget(content: DeltaStatic): Widget {
  return {
    id: 'rich-text-widget',
    dashboardId: 'dashboard',
    datachartId: '',
    relations: [],
    viewIds: [],
    parentId: '',
    config: {
      version: '1.0.0',
      name: '富文本',
      boardType: 'auto',
      clientId: 'rich-text-widget-client',
      index: 1,
      type: 'media',
      originalType: 'richText',
      lock: false,
      customConfig: {},
      content: {
        type: 'richText',
        richText: {
          content,
        },
      },
      rect: { x: 0, y: 0, width: 1, height: 1 },
      pRect: { x: 0, y: 0, width: 1, height: 1 },
    },
  } as Widget;
}

function createWidgetInfo(editing: boolean): WidgetInfo {
  return {
    id: 'rich-text-widget',
    loading: false,
    editing,
    rendered: true,
    inLinking: false,
    selected: false,
    pageInfo: {},
    errInfo: {
      request: '',
      interaction: '',
    },
  };
}

describe('RichTextWidgetCore Quill 2 smoke', () => {
  afterEach(() => {
    document.body.innerHTML = '';
    dispatchMock.mockClear();
    clearActiveWidgetsMock.mockClear();
  });

  test('应在 Dashboard 富文本 Widget 展示层渲染 Delta 内容', async () => {
    render(
      <RichTextWidgetCore
        widget={createWidget({
          ops: [{ insert: 'yu-bi dashboard rich text\n' }],
        })}
        widgetInfo={createWidgetInfo(false)}
        boardEditing={false}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('yu-bi dashboard rich text')).toBeInTheDocument();
    });
  });

  test('应在编辑弹窗挂载 Quill 2 运行时并保存变更内容', async () => {
    const { rerender } = render(
      <RichTextWidgetCore
        widget={createWidget({
          ops: [{ insert: '旧内容\n' }],
        })}
        widgetInfo={createWidgetInfo(true)}
        boardEditing={true}
      />,
    );

    await waitFor(() => {
      expect(
        screen.getAllByText('旧内容', { selector: '.ql-editor *' }).length,
      ).toBeGreaterThan(0);
    });

    const editor = document.querySelector('.ant-modal .ql-editor');
    expect(editor).not.toBeNull();
    if (!editor) {
      throw new Error('富文本编辑弹窗未挂载 Quill 编辑器');
    }

    const quillContainer = document.querySelector('.ant-modal .ql-container');
    expect(quillContainer).not.toBeNull();
    if (!quillContainer) {
      throw new Error('富文本编辑弹窗未挂载 Quill 容器');
    }

    const quill = Quill.find(quillContainer) as QuillInstance;
    act(() => {
      quill.setText('新内容\n', 'user');
    });

    fireEvent.click(screen.getByText('OK'));

    rerender(
      <RichTextWidgetCore
        widget={createWidget({
          ops: [{ insert: '旧内容\n' }],
        })}
        widgetInfo={createWidgetInfo(false)}
        boardEditing={true}
      />,
    );

    await waitFor(() => {
      expect(dispatchMock).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'editBoard/changeMediaWidgetConfig',
          payload: expect.objectContaining({
            id: 'rich-text-widget',
            mediaWidgetContent: expect.objectContaining({
              richText: {
                content: expect.objectContaining({
                  ops: [{ insert: '新内容\n' }],
                }),
              },
            }),
          }),
        }),
      );
    });
    expect(clearActiveWidgetsMock).toHaveBeenCalledTimes(1);
  });
});
