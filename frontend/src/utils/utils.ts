import { message, TreeDataNode, TreeNodeProps } from 'antd';
import { ColumnRole } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { AxiosError, AxiosResponse } from 'axios';
import classnames from 'classnames';
import i18next from 'i18next';
import { ReactElement } from 'react';
import {
  FONT_FAMILY,
  FONT_SIZE_BODY,
  FONT_WEIGHT_REGULAR,
} from 'styles/StyleConstants';
import { APIResponse } from 'types';
import { SaveFormModel } from '../app/pages/MainPage/pages/VizPage/SaveFormContext';
import { removeToken } from './auth';

type ErrorLike = {
  message?: string;
};

type AxiosErrorLike = ErrorLike & {
  response?: AxiosResponse<APIResponse<unknown>>;
};

type RejectWithValue = (value: unknown) => unknown;

function getRandomByte(): number {
  return Math.floor(Math.random() * 256);
}

function createUuidFromRandomBytes(randomBytes: number[]) {
  const bytes = randomBytes.slice(0, 16);

  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;

  const hex = bytes.map(byte => byte.toString(16).padStart(2, '0')).join('');

  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20, 32),
  ].join('-');
}

export function uuidv4() {
  if (
    typeof crypto !== 'undefined' &&
    typeof crypto.randomUUID === 'function'
  ) {
    return crypto.randomUUID();
  }

  if (
    typeof crypto !== 'undefined' &&
    typeof crypto.getRandomValues === 'function'
  ) {
    return createUuidFromRandomBytes(
      Array.from(crypto.getRandomValues(new Uint8Array(16))),
    );
  }

  return createUuidFromRandomBytes(
    Array.from({ length: 16 }, () => getRandomByte()),
  );
}

// For environments that do not support crypto.getRandomValues, such as nashorn.
export function universalUUID() {
  return uuidv4();
}

function asAxiosErrorLike(error: unknown): AxiosErrorLike | undefined {
  if (typeof error !== 'object' || error === null) {
    return undefined;
  }

  return error as AxiosErrorLike;
}

function getErrorLikeMessage(error: unknown): string | undefined {
  return asAxiosErrorLike(error)?.message;
}

export function errorHandle<T>(error: T): T {
  const errorLike = asAxiosErrorLike(error);

  if (errorLike?.response) {
    // AxiosError
    const { response } = errorLike;
    switch (response?.status) {
      case 401:
        message.error({ key: '401', content: String(i18next.t('global.401')) });
        removeToken();
        break;
      default:
        message.error(response?.data.message || errorLike.message);
        break;
    }
  } else if (errorLike?.message) {
    // Error
    message.error(errorLike.message);
  } else {
    message.error(String(error));
  }
  return error;
}

export function getErrorMessage(error: unknown) {
  if (typeof error === 'string') {
    return error;
  }

  const errorLike = asAxiosErrorLike(error);
  if (errorLike?.response) {
    const { response } = errorLike;
    switch (response?.status) {
      case 401:
        removeToken();
        return String(i18next.t('global.401'));
      default:
        return response?.data.message || errorLike.message;
    }
  }
  return getErrorLikeMessage(error);
}

export function reduxActionErrorHandler(errorAction: {
  payload?: unknown;
  error?: ErrorLike;
}) {
  if (errorAction?.payload) {
    message.error(String(errorAction.payload));
  } else if (errorAction?.error) {
    message.error(errorAction?.error.message);
  }
}

export function rejectHandle(error: unknown, rejectWithValue: RejectWithValue) {
  const errorLike = asAxiosErrorLike(error);

  if (errorLike?.response?.status === 401) {
    removeToken();
  }

  if (errorLike?.response) {
    return rejectWithValue(errorLike.response.data.message);
  } else {
    return rejectWithValue(errorLike?.message);
  }
}

export const mergeClassNames = (origin, added) =>
  classnames({ [origin]: !!origin, [added]: true });

export function stopPPG(e) {
  e.stopPropagation();
}

export type ListTreeNode<T> = T & {
  key: string;
  title: string;
  value: string;
  path: string[];
  icon?: ReactElement | ((props: TreeNodeProps) => ReactElement);
  disabled?: boolean;
  selectable?: boolean;
  children?: Array<ListTreeNode<T>>;
  isLeaf?: boolean;
};

export function listToTree<
  T extends {
    id: string;
    name: string;
    parentId: string | null;
    isFolder: boolean;
    index: number | null;
  },
>(
  list: undefined | T[],
  parentId: null | string = null,
  parentPath: string[] = [],
  options?: {
    getIcon?: (
      o: T,
    ) => ReactElement | ((props: TreeNodeProps) => ReactElement) | undefined;
    getDisabled?: (o: T, path: string[]) => boolean;
    getSelectable?: (o: T) => boolean;
    filter?: (path: string[], o: T) => boolean;
  },
): undefined | Array<ListTreeNode<T>> {
  if (!list) {
    return list;
  }

  const treeNodes: Array<ListTreeNode<T>> = [];
  const childrenList: T[] = [];

  list.forEach(o => {
    const path = parentPath.concat(o.id);
    if (options?.filter && !options.filter(path, o)) {
      return false;
    }
    if (o.parentId === parentId) {
      treeNodes.push({
        ...o,
        key: o.id,
        title: o.name,
        value: o.id,
        path,
        ...(options?.getIcon && { icon: options.getIcon(o) }),
        ...(options?.getDisabled && { disabled: options.getDisabled(o, path) }),
        ...(options?.getSelectable && { selectable: options.getSelectable(o) }),
      });
    } else {
      childrenList.push(o);
    }
  });

  treeNodes.sort((a, b) => Number(a.index) - Number(b.index));

  return treeNodes.map(node => {
    const children = listToTree(childrenList, node.key, node.path, options);
    return children?.length ? { ...node, children } : { ...node, isLeaf: true };
  });
}

export function findTreeNode<
  T extends {
    key: string | number;
    children?: T[];
  },
>(path: string[], nodes: T[] | undefined): T | undefined {
  if (path.length > 0) {
    const currentNode = nodes?.find(({ key }) => key === path[0]);
    return path.length > 1
      ? findTreeNode(path.slice(1), currentNode?.children)
      : currentNode;
  }
}

export const loopTree = (data, key: string, keyname: string, callback) => {
  for (let i = 0; i < data.length; i++) {
    if (data[i].key === key) {
      return callback(data[i], i, data);
    }
    if (data[i].children) {
      loopTree(data[i].children, key, keyname, callback);
    }
  }
};

export const onDropTreeFn = ({ info, treeData, callback }) => {
  const dropKey = info.node.key; //落下的key
  const dragKey = info.dragNode.key; //拖动的key
  const dropPos = info.node.pos.split('-');
  const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
  const data = treeData || [];
  let dragObj,
    dropArr,
    dropIndex,
    index = 0;

  loopTree(data, dragKey, 'key', item => {
    dragObj = item;
  });

  loopTree(data, dropKey, 'key', (item, idx, arr) => {
    dropArr = arr;
    dropIndex = idx;
  });
  if (!info.dropToGap && !dropArr[dropIndex].isFolder) {
    //判断不能移动到非目录下面
    return false;
  }

  if (
    dropArr[dropIndex].parentId === dragObj.id ||
    (dropArr[dropIndex].isFolder && dropArr[dropIndex].id === dragObj.id)
  ) {
    return false;
  }

  if (!info.dropToGap) {
    //如果移动到二级目录里面的第一个，获取到该目录children中[0]元素的index-1
    index = dropArr[dropIndex].children
      ? dropArr[dropIndex].children[0]?.index - 1
      : 0;
  } else if (dropPosition === -1) {
    // 移动到第一个
    index = dropArr[dropIndex] ? dropArr[dropIndex].index - 1 : 0;
  } else if (dropIndex === dropArr.length - 1) {
    // 移动到最后一个
    index = dropArr[dropArr.length - 1].index + 1;
  } else {
    //中间
    if (!dropArr[dropIndex].index && !dropArr[dropIndex + 1].index) {
      index = dropArr[dropArr.length - 1].index + 1;
    } else {
      index = (dropArr[dropIndex].index + dropArr[dropIndex + 1].index) / 2;
    }
  }
  let { id } = dragObj,
    parentId = !info.dropToGap
      ? dropArr[dropIndex].id
      : dropArr[dropIndex].parentId || null;
  //如果移动到二级目录里面的第一个，就用当前目录的id,如果不是就用文件的parentId
  callback(id, parentId, index);
};

export const getInsertedNodeIndex = (
  AddData: Omit<SaveFormModel, 'config'> & { config?: object | string },
  viewData?: Array<{ parentId?: string | null; index?: number | null }>,
) => {
  let index: number = 0;
  if (viewData?.length) {
    let IndexArr = viewData
      .filter(v => v.parentId == AddData.parentId)
      .map(val => Number(val.index) || 0);
    index = IndexArr?.length ? Math.max(...IndexArr) + 1 : 0;
  }
  return index;
};

export function getPath<T extends { id: string; parentId: string | null }>(
  list: T[],
  item: T,
  rootId: string,
  path: string[] = [],
) {
  if (!item?.parentId) {
    if (item) {
      return [rootId].concat(item.id).concat(path);
    }
    return [rootId].concat(path);
  } else {
    const parent = list.find(({ id }) => id === item.parentId)!;
    return getPath(list, parent, rootId, [item.id].concat(path));
  }
}

export function filterListOrTree<T>(
  dataSource: T[],
  keywords: string,
  filterFunc: (keywords: string, data: T) => boolean,
  filterLeaf?: boolean, // 是否展示所有叶子节点
): T[] {
  return keywords
    ? dataSource.reduce<T[]>((filtered, d) => {
        const treeNode = d as T & {
          children?: Array<T & { isLeaf?: boolean }>;
        };
        const isMatch = filterFunc(keywords, d);
        let isChildrenMatch: T[] | undefined;
        if (filterLeaf && treeNode.children?.every(c => c.isLeaf)) {
          isChildrenMatch =
            isMatch || treeNode.children.some(c => filterFunc(keywords, c))
              ? treeNode.children
              : void 0;
        } else {
          isChildrenMatch =
            treeNode.children &&
            filterListOrTree(
              treeNode.children,
              keywords,
              filterFunc,
              filterLeaf,
            );
        }
        if (isMatch || (isChildrenMatch && isChildrenMatch.length > 0)) {
          filtered.push({ ...treeNode, children: isChildrenMatch } as T);
        }
        return filtered;
      }, [])
    : dataSource;
}

export function getExpandedKeys<T>(nodes: T[]) {
  return nodes.reduce<string[]>((keys, node) => {
    const { key, children } = node as T & TreeDataNode;
    if (Array.isArray(children) && children.length) {
      return keys
        .concat(key as string)
        .concat(children ? getExpandedKeys(children) : []);
    }
    return keys;
  }, []);
}

let utilCanvas: null | HTMLCanvasElement = null;

export const getTextWidth = (
  text: string,
  fontWeight: string = `${FONT_WEIGHT_REGULAR}`,
  fontSize: string = FONT_SIZE_BODY,
  fontFamily: string = FONT_FAMILY,
): number => {
  const canvas = utilCanvas || (utilCanvas = document.createElement('canvas'));
  const context = canvas.getContext('2d');
  if (context) {
    context.font = `${fontWeight} ${fontSize} ${fontFamily}`;
    const metrics = context.measureText(text);
    return Math.ceil(metrics.width);
  }
  return 0;
};

export function getDiffParams<T extends { id?: string }>(
  origin: T[],
  changed: T[],
  matchFunc: (originElement: T, changedElement: T) => boolean,
  compareFunc: (originElement: T, changedElement: T) => boolean,
  continueFunc?: (originElement: T) => boolean,
) {
  let reserved: T[] = [];
  let created: T[] = [];
  let updated: T[] = [];
  let deleted: T[] = [];

  for (let i = 0; i < origin.length; i += 1) {
    /**
     * 由于 fastDeleteArrayElement 会改变数组元素位置，因此代码中使用 origin[i]、
     * changed[j] 即时获取对应下标元素，而非使用变量暂存
     */
    if (continueFunc && continueFunc(origin[i])) {
      reserved.push(origin[i]);
      fastDeleteArrayElement(origin, i);
      i -= 1;
      continue;
    }

    for (let j = 0; j < changed.length; j += 1) {
      if (matchFunc(origin[i], changed[j])) {
        const updatedElement = { ...changed[j], id: origin[i].id };
        if (compareFunc(origin[i], changed[j])) {
          updated.push(updatedElement);
        }
        reserved.push(updatedElement);
        fastDeleteArrayElement(origin, i);
        fastDeleteArrayElement(changed, j);
        i -= 1;
        break;
      }
    }
  }

  created = [...changed];
  deleted = [...origin];

  return {
    created,
    deleted,
    updated,
    reserved,
  };
}

export function fastDeleteArrayElement<T>(arr: T[], index: number) {
  arr[index] = arr[arr.length - 1];
  arr.pop();
}

export function newIssueUrl({ type, ...options }) {
  const repoUrl = `https://${type}.com/running-elephant/datart`;
  let issuesUrl = '';

  if (repoUrl) {
    issuesUrl = repoUrl;
  } else {
    throw new Error(
      'You need to specify either the `repoUrl` option or both the `user` and `repo` options',
    );
  }

  const url = new URL(`${issuesUrl}/issues/new`);

  const types =
    type === 'gitee'
      ? ['description', 'title']
      : [
          'body',
          'title',
          'labels',
          'template',
          'milestone',
          'assignee',
          'projects',
        ];

  for (const type of types) {
    let value = options[type];

    if (value === undefined) {
      continue;
    }

    if (type === 'labels' || type === 'projects') {
      if (!Array.isArray(value)) {
        throw new TypeError(`The \`${type}\` option should be an array`);
      }

      value = value.join(',');
    }

    url.searchParams.set(type, value);
  }

  return url.toString();
}

type ModelTreePageType = 'analysisPage' | 'viewPage';
type ModelTreeColumn = ChartDataViewMeta & {
  displayName?: string;
};
type ModelTreeTableNode = ChartDataViewMeta & {
  role: ColumnRole.Table;
  children: ModelTreeColumn[];
  id?: string;
  index?: number;
};

export function modelListFormsTreeByTableName(
  model: ChartDataViewMeta[] | undefined,
  type: ModelTreePageType,
): ModelTreeTableNode[] {
  const tableNameList: string[] = [];
  const columnNameObj: Record<string, ModelTreeColumn[]> = {};
  const columnTreeData: ModelTreeTableNode[] = [];

  model?.forEach(v => {
    const path = v.path;
    if (!path?.length) {
      return;
    }
    const tableName = path.slice(0, path.length - 1).join('.');
    if (!tableNameList.includes(tableName)) {
      tableNameList.push(tableName);
    }
  });

  model?.forEach(v => {
    const path = v.path;
    if (!path?.length) {
      return;
    }
    const tableName = path.slice(0, path.length - 1).join('.');
    const fieldName = path[path.length - 1];
    if (tableNameList.includes(tableName)) {
      const columnNameArr = columnNameObj[tableName] || [];
      columnNameObj[tableName] = columnNameArr.concat([
        { ...v, displayName: fieldName },
      ]);
    }
  });

  tableNameList.sort((a, b) => a.localeCompare(b));

  tableNameList.forEach((v, i) => {
    const treeData = {
      name: v,
      category: 'hierarchy',
      role: ColumnRole.Table,
      subType: undefined,
      type: 'STRING',
      children: columnNameObj[v],
    } as ModelTreeTableNode;

    if (type === 'analysisPage') {
      treeData.id = v;
    }
    if (type === 'viewPage') {
      treeData.index = i;
    }

    columnTreeData.push(treeData);
  });

  return columnTreeData;
}
