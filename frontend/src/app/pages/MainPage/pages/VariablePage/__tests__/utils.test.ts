import { describe, expect, test } from 'vitest';
import { DateFormat } from 'app/constants';
import { VariableTypes, VariableValueTypes } from '../constants';
import { serializeVariableDefaultValue } from '../utils';

describe('serializeVariableDefaultValue', () => {
  test('should format date default values with variable date format', () => {
    expect(
      serializeVariableDefaultValue({
        name: 'dt',
        orgId: 'org-1',
        type: VariableTypes.Query,
        valueType: VariableValueTypes.Date,
        encrypt: false,
        permission: 1,
        defaultValue: ['2024-01-02 03:04:05'],
        expression: false,
        dateFormat: DateFormat.DateTime,
      }),
    ).toEqual(['2024-01-02 03:04:05']);
  });

  test('should keep expression default values unchanged', () => {
    expect(
      serializeVariableDefaultValue({
        name: 'expr',
        orgId: 'org-1',
        type: VariableTypes.Query,
        valueType: VariableValueTypes.Date,
        encrypt: false,
        permission: 1,
        defaultValue: ['now()'],
        expression: true,
        dateFormat: DateFormat.DateTime,
      }),
    ).toEqual(['now()']);
  });

  test('should keep non-date default values unchanged', () => {
    expect(
      serializeVariableDefaultValue({
        name: 'name',
        orgId: 'org-1',
        type: VariableTypes.Query,
        valueType: VariableValueTypes.String,
        encrypt: false,
        permission: 1,
        defaultValue: ['demo'],
        expression: false,
        dateFormat: DateFormat.Date,
      }),
    ).toEqual(['demo']);
  });
});
