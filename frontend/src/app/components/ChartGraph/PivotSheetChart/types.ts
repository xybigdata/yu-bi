import {
  Node,
  Palette,
  RowCellCollapsedParams,
  S2DataConfig,
  S2Theme,
  SpreadSheet,
  TargetCellInfo,
} from '@antv/s2';
import { SheetComponentOptions } from '@antv/s2-react';
export interface AndvS2Config {
  dataCfg?: S2DataConfig;
  options: SheetComponentOptions;
  theme?: S2Theme;
  palette?: Palette;
  onRowCellCollapseTreeRows?: (val: RowCellCollapsedParams) => void;
  onCollapseRowsAll?: (isCollapsed: boolean) => void;
  onSelected?: (cells: any[]) => void;
  onDataCellClick?: (data: TargetCellInfo) => void;
  getSpreadSheet?: (spreadsheet: SpreadSheet) => void;
}
