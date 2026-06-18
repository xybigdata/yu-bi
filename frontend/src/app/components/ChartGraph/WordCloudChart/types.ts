import { ChartRowData } from 'app/types/Chart';

export interface WordCloudConfig {
  drawOutOfBound: boolean;
  shape: string;
  width: string;
  height: string;
  left: string;
  top: string;
  right: string;
  bottom: string;
}

export interface WordCloudLabelConfig {
  sizeRange: number[];
  rotationRange: number[];
  rotationStep: number;
  gridSize: number;
  emphasis: {
    focus: string;
    textStyle: {
      textShadowBlur: number;
      textShadowColor: string;
    };
  };
  data?:
    | {
        name: string;
        rowData: ChartRowData;
        textStyle: { opacity?: number } & Record<string, unknown>;
        value: string;
      }[]
    | undefined;
}
