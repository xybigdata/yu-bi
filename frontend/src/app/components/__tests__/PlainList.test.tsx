import { render, screen } from '@testing-library/react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test } from 'vitest';

import { themes } from 'styles/theme/themes';

import { PlainList } from '../PlainList';

const renderWithTheme = (node: React.ReactNode) =>
  render(<ThemeProvider theme={themes.light}>{node}</ThemeProvider>);

describe('PlainList', () => {
  test('should render data source items with stable row keys', () => {
    const { container } = renderWithTheme(
      <PlainList
        dataSource={[
          { id: 'first', name: 'First item' },
          { id: 'second', name: 'Second item' },
        ]}
        rowKey={item => item.id}
        renderItem={item => <li>{item.name}</li>}
      />,
    );

    expect(screen.getByText('First item')).toBeInTheDocument();
    expect(screen.getByText('Second item')).toBeInTheDocument();
    expect(container.querySelectorAll('.ant-list-items > li')).toHaveLength(2);
  });

  test('should expose loading state without hiding the current items', () => {
    const { container } = renderWithTheme(
      <PlainList loading={{ spinning: true, indicator: <span>Loading</span> }}>
        <li>Existing item</li>
      </PlainList>,
    );

    expect(screen.getByText('Existing item')).toBeInTheDocument();
    expect(screen.getByText('Loading')).toBeInTheDocument();
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull();
  });
});
