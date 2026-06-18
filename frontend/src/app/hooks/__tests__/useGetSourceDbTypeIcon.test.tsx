import { renderHook } from '@testing-library/react';
import { ReactElement } from 'react';
import useGetSourceDbTypeIcon from '../useGetSourceDbTypeIcon';

const createSource = (overrides = {}) => ({
  config: '{}',
  id: 'source-1',
  index: null,
  isFolder: false,
  name: 'source',
  orgId: 'org-1',
  parentId: null,
  type: 'JDBC',
  deleteLoading: false,
  ...overrides,
});

describe('useGetSourceDbTypeIcon Test', () => {
  test('should fallback to default avatar when jdbc config is invalid json', () => {
    const { result } = renderHook(() => useGetSourceDbTypeIcon());

    expect(() =>
      result.current(createSource({ config: '{invalid-json}' })),
    ).not.toThrow();

    const icon = result.current(
      createSource({ config: '{invalid-json}' }),
    ) as ReactElement;
    expect(icon.props.children).toBe('');
  });

  test('should fallback to default avatar when jdbc config is non-object json', () => {
    const { result } = renderHook(() => useGetSourceDbTypeIcon());

    const icon = result.current(
      createSource({ config: 'true' }),
    ) as ReactElement;

    expect(icon.props.children).toBe('');
  });
});
