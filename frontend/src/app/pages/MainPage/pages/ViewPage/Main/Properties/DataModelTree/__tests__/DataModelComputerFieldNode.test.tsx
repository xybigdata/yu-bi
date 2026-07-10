import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { DataViewFieldType } from 'app/constants';
import { themes } from 'styles/theme/themes';

import DataModelComputerFieldNode, {
  DATA_MODEL_COMPUTED_FIELD_MENU_ITEM_CLASS,
  DATA_MODEL_COMPUTED_FIELD_MENU_POPUP_CLASS,
} from '../DataModelComputerFieldNode';

vi.mock('app/hooks/useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

const renderNode = () =>
  render(
    <ThemeProvider theme={themes.light}>
      <DataModelComputerFieldNode
        node={{
          name: 'test',
          type: DataViewFieldType.STRING,
        }}
        menuClick={vi.fn()}
      />
    </ThemeProvider>,
  );

describe('DataModelComputerFieldNode', () => {
  test('should use compact computed field menu overlay and consistent item layout', async () => {
    const { container } = renderNode();

    fireEvent.click(container.querySelector('.anticon-more')!);

    await waitFor(() => {
      expect(
        document.querySelector(
          `.${DATA_MODEL_COMPUTED_FIELD_MENU_POPUP_CLASS}`,
        ),
      ).toBeInTheDocument();
    });

    expect(screen.getByText('edit')).toBeInTheDocument();
    expect(screen.getByText('delete')).toBeInTheDocument();
    expect(
      document.querySelectorAll(
        `.${DATA_MODEL_COMPUTED_FIELD_MENU_ITEM_CLASS}`,
      ),
    ).toHaveLength(2);
  });
});
