import { describe, expect, test } from 'vitest';
import { DateFormat } from 'app/constants';
import { VariableTypes, VariableValueTypes } from '../constants';
import {
  parseVariableDefaultValue,
  parseVariableRelationValue,
  serializeVariableDefaultValue,
  serializeVariableRelationValue,
} from '../utils';

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

describe('serializeVariableRelationValue', () => {
  test('should format relation date values with variable date format', () => {
    expect(
      serializeVariableRelationValue(['2024-01-02 03:04:05'], {
        valueType: VariableValueTypes.Date,
        dateFormat: DateFormat.Date,
      }),
    ).toEqual(['2024-01-02']);
  });

  test('should keep non-date relation values unchanged', () => {
    const values = ['demo'];

    expect(
      serializeVariableRelationValue(values, {
        valueType: VariableValueTypes.String,
        dateFormat: DateFormat.DateTime,
      }),
    ).toBe(values);
  });
});

describe('parseVariableRelationValue', () => {
  test('should parse relation raw json value', () => {
    expect(parseVariableRelationValue('["2024-01-02 03:04:05"]')).toEqual([
      '2024-01-02 03:04:05',
    ]);
  });

  test('should keep empty relation raw value as undefined', () => {
    expect(parseVariableRelationValue(undefined)).toBeUndefined();
    expect(parseVariableRelationValue(null)).toBeUndefined();
    expect(parseVariableRelationValue('')).toBeUndefined();
  });

  test('should ignore invalid or non-array relation raw value', () => {
    expect(parseVariableRelationValue('{invalid-json}')).toBeUndefined();
    expect(parseVariableRelationValue('true')).toBeUndefined();
  });
});

describe('parseVariableDefaultValue', () => {
  test('should parse default value raw json', () => {
    expect(parseVariableDefaultValue('["demo"]')).toEqual(['demo']);
  });

  test('should keep empty default value as empty array', () => {
    expect(parseVariableDefaultValue(undefined)).toEqual([]);
    expect(parseVariableDefaultValue(null)).toEqual([]);
    expect(parseVariableDefaultValue('')).toEqual([]);
  });

  test('should ignore invalid or non-array default value', () => {
    expect(parseVariableDefaultValue('{invalid-json}')).toEqual([]);
    expect(parseVariableDefaultValue('true')).toEqual([]);
  });
});
