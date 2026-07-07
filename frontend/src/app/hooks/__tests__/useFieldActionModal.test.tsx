import { renderHook } from '@testing-library/react';
import {
  ChartDataSectionFieldActionType,
  ChartDataViewFieldCategory,
  DataViewFieldType,
} from 'app/constants';
import { describe, expect, test, vi } from 'vitest';
import { FILTER_FORM_WIDE_CONTROL_WIDTH } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/layout';
import useFieldActionModal from '../useFieldActionModal';

const stateModalMock = vi.hoisted(() => ({
  show: vi.fn(),
}));

vi.mock('../useI18NPrefix', () => ({
  default: () => (key: string) => key,
}));

vi.mock('../useStateModal', () => ({
  default: () => [stateModalMock.show, null],
  StateModalSize: {
    XSMALL: 520,
    SMALL: 600,
    MIDDLE: 1000,
    LARGE: 1600,
    XLARGE: 2000,
  },
}));

vi.mock(
  'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction',
  () => ({
    default: {
      FilterAction: () => null,
    },
  }),
);

describe('useFieldActionModal', () => {
  test('should use stable wide width for filter action modal', () => {
    const { result } = renderHook(() =>
      useFieldActionModal({ i18nPrefix: 'viz.common' }),
    );

    result.current[0](
      'field-1',
      ChartDataSectionFieldActionType.Filter,
      {
        key: 'data',
        rows: [
          {
            uid: 'field-1',
            colName: 'country_name',
            type: DataViewFieldType.STRING,
            category: ChartDataViewFieldCategory.Field,
          },
        ],
      },
      vi.fn(),
    );

    expect(stateModalMock.show).toHaveBeenCalledWith(
      expect.objectContaining({
        modalSize: 980,
      }),
    );
    expect(FILTER_FORM_WIDE_CONTROL_WIDTH).toBe(680);
  });
});
