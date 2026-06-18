import { ChartDataConfig } from 'app/types/ChartConfig';
import { I18NTranslateOptions } from 'app/hooks/useI18NPrefix';
import { ReactNode } from 'react';
import { FormGroupLayoutMode } from './constants';

export interface ItemLayoutProps<T> {
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
    value: T | any,
    needRefresh?: boolean,
  ) => void;
  dataConfigs?: ChartDataConfig[];
  flatten?: boolean;
  context?: any;
}

export interface FormGeneratorLayoutProps<T> extends ItemLayoutProps<T> {
  mode?: FormGroupLayoutMode; // NOTE: inner means this group panel whether wrap into a panel. Default is outer, no parent panel.
  dependency?: string;
}
