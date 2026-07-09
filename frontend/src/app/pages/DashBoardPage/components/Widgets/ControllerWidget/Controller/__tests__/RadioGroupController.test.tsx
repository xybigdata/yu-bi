import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';

import { RadioGroupController } from '../RadioGroupController';

describe('RadioGroupController', () => {
  test('should render default options as radio controls with text', () => {
    const onChange = vi.fn();
    const { container } = render(
      <RadioGroupController
        onChange={onChange}
        options={[
          { label: 'AWG', value: 'AWG' },
          { label: 'BMD', value: 'BMD' },
        ]}
        value="AWG"
      />,
    );

    expect(container.querySelectorAll('.ant-radio-wrapper')).toHaveLength(2);
    expect(container.querySelector('.ant-radio-button-wrapper')).toBeNull();

    fireEvent.click(screen.getByText('BMD'));

    expect(onChange).toHaveBeenCalledWith(['BMD']);
  });

  test('should clear selected value when clicking the checked option again', () => {
    const onChange = vi.fn();
    render(
      <RadioGroupController
        onChange={onChange}
        options={[
          { label: 'AWG', value: 'AWG' },
          { label: 'BMD', value: 'BMD' },
        ]}
        value="AWG"
      />,
    );

    fireEvent.click(screen.getByText('AWG'));

    expect(onChange).toHaveBeenCalledWith([]);
  });
});
