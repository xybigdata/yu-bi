import { ChartDataConfig, ChartStyleSectionRow } from 'app/types/ChartConfig';
import { I18NTranslateOptions } from 'app/hooks/useI18NPrefix';
import { ReactNode } from 'react';
import { FormGroupLayoutMode } from './constants';

export type FormGeneratorValue =
  | string
  | number
  | boolean
  | null
  | undefined
  | string[]
  | number[]
  | boolean[]
  | ChartStyleSectionRow
  | Record<string, unknown>;

export interface ItemLayoutProps<
  T,
  TValue = FormGeneratorValue,
  TContext = unknown,
> {
  children?: ReactNode;
  ancestors: number[];
  data: T;
  translate?: (
    title: string,
    disablePrefix?: boolean,
    options?: I18NTranslateOptions,
  ) => string;
  onChange?: (
    ancestors: number[],
    value: TValue,
    needRefresh?: boolean,
  ) => void;
  dataConfigs?: ChartDataConfig[];
  flatten?: boolean;
  context?: TContext;
}

export interface FormGeneratorLayoutProps<
  T,
  TValue = FormGeneratorValue,
  TContext = unknown,
> extends ItemLayoutProps<T, TValue, TContext> {
  mode?: FormGroupLayoutMode; // NOTE: inner means this group panel whether wrap into a panel. Default is outer, no parent panel.
  dependency?: string;
}
