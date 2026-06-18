import type {
  Node,
  Palette,
  RowCellCollapsedParams,
  CellSelectedHandler,
  S2DataConfig,
  S2Theme,
  SpreadSheet,
  TargetCellInfo,
} from '@antv/s2';
import type { SheetComponentOptions } from '@antv/s2-react';
export interface AndvS2Config {
  dataCfg?: S2DataConfig;
  options: SheetComponentOptions;
  theme?: S2Theme;
  palette?: Palette;
  onRowCellCollapseTreeRows?: (val: RowCellCollapsedParams) => void;
  onCollapseRowsAll?: (isCollapsed: boolean) => void;
  onSelected?: CellSelectedHandler;
  onDataCellClick?: (data: TargetCellInfo) => void;
  getSpreadSheet?: (spreadsheet: SpreadSheet) => void;
}
