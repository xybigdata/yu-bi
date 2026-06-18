import { ChartRowData } from 'app/types/Chart';
import { BorderStyle, FontStyle, LabelStyle } from '../../../types/ChartConfig';
import { EChartsOption } from '../echartsRuntime';

export type GeoInfo = {
  map?: string;
  roam?: boolean | 'move' | 'scale';
  emphasis?: {
    focus?: string;
    itemStyle?: {
      areaColor: string;
    };
  };
  itemStyle?: {
    areaColor: string;
  } & BorderStyle;
  regions?: { name?: string; itemStyle?: Record<string, unknown> }[];
  zoom?: number;
  center?: number[] | undefined;
} & LabelStyle;

export interface GeoVisualMapStyle {
  type: string;
  seriesIndex: number;
  dimension: undefined | number;
  show: boolean;
  orient: string;
  align: string;
  itemWidth: number;
  itemHeight: number;
  inRange: {
    color: string[];
  };
  text: string[];
  min: number;
  max: number;
  textStyle: FontStyle;
  formatter: (value) => string;
  bottom?: number | string;
  right?: number | string;
  top?: number | string;
  left?: number | string;
}

export interface GeoSeries {
  type: string;
  roam?: boolean;
  map?: string;
  geoIndex?: number;
  emphasis?: {
    label?: {
      show?: boolean;
    };
    disabled?: boolean;
  };
  select?: {
    disabled?: boolean;
  };
  data: Array<{
    rowData: ChartRowData;
    name: string;
    value: string;
    visualMap?: boolean;
  }>;
}

export type MetricAndSizeSeriesStyle = {
  data: Array<{
    rowData: ChartRowData;
    name: string;
    value: Array<number[] | number | string>;
  }>;
  type: string;
  zlevel: number;
  coordinateSystem: string;
  symbol: string;
  symbolSize: (value: Array<number | string | undefined>) => number;
  emphasis: {
    label: {
      show: boolean;
    };
  };
} & LabelStyle;

export interface MapOption extends EChartsOption {
  geo?: GeoInfo;
  visualMap?: GeoVisualMapStyle[];
  series?: Array<GeoSeries | MetricAndSizeSeriesStyle>;
  tooltip?: { trigger: string; formatter: (params: unknown) => string };
  toolbox?: {
    orient: string;
    top: string;
    feature: {
      [p: string]: {
        onclick: () => void;
        show: boolean;
        icon: string;
        title?: string;
      };
    };
    left: string;
    show: boolean;
  };
}
