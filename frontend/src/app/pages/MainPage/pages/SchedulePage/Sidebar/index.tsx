import {
  ClearOutlined,
  DeleteOutlined,
  FolderFilled,
  FolderOpenFilled,
  MailOutlined,
  SelectOutlined,
  SettingOutlined,
  WechatOutlined,
} from '@ant-design/icons';
import { message } from 'antd';
import {
  ListNav,
  ListPane,
  ListTitle,
  SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
  SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
} from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { SidebarCollapseButton } from 'app/pages/MainPage/components/SidebarCollapseButton';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import {
  memo,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import {
  RecycleBatchManager,
  RecycleBinHandle,
  RecycleBinManager,
} from 'app/features/recycle';
import styled from 'styled-components';
import { LEVEL_5, SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { JobTypes } from '../constants';
import { useToScheduleDetails } from '../hooks';
import { SaveForm } from '../SaveForm';
import { SaveFormContext } from '../SaveFormContext';
import { makeSelectScheduleTree, selectSchedules } from '../slice/selectors';
import { addSchedule, getSchedules } from '../slice/thunks';
import { ScheduleSimpleViewModel } from '../slice/types';
import { ScheduleList } from './ScheduleList';
import { useScheduleRouteParams } from '../hooks';

interface SidebarProps extends I18NComponentProps {
  isDragging: boolean;
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
}

export const Sidebar = memo(
  ({ isDragging, sliderVisible, handleSliderVisible }: SidebarProps) => {
    const dispatch = useAppDispatch();
    const { scheduleId } = useScheduleRouteParams();
    const orgId = useSelector(selectOrgId);
    const scheduleData = useSelector(selectSchedules);
    const { showSaveForm } = useContext(SaveFormContext);
    const t = useI18NPrefix('schedule.sidebar');
    const tg = useI18NPrefix('global');
    const [batchMode, setBatchMode] = useState(false);
    const recycleRef = useRef<RecycleBinHandle>(null);

    const handleBatchCompleted = useCallback(() => {
      setBatchMode(false);
      dispatch(getSchedules(orgId));
    }, [dispatch, orgId]);

    const selectScheduleTree = useMemo(makeSelectScheduleTree, []);
    const getIcon = useCallback(
      ({ isFolder, type }: ScheduleSimpleViewModel) =>
        isFolder ? (
          p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />)
        ) : type === JobTypes.Email ? (
          <MailOutlined />
        ) : (
          <WechatOutlined />
        ),
      [],
    );
    const getDisabled = useCallback(
      ({ deleteLoading }: ScheduleSimpleViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectScheduleTree(state, { getIcon, getDisabled }),
    );

    const { filteredData: scheduleList, debouncedSearch: listSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const { toDetails } = useToScheduleDetails();
    const toAdd = useCallback(
      ({ key }) => {
        switch (key) {
          case 'add':
            toDetails(orgId, 'add');
            break;
          case 'folder':
            showSaveForm({
              scheduleType: 'folder',
              type: CommonFormTypes.Add,
              open: true,
              simple: false,
              parentIdLabel: t('scheduleList.parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, scheduleData);
                dispatch(
                  addSchedule({
                    params: {
                      ...values,
                      parentId: values.parentId || null,
                      index,
                      orgId,
                      isFolder: true,
                    },
                    resolve: () => {
                      onClose();
                      message.success(t('index.addSuccess'));
                    },
                  }),
                );
              },
            });
            break;
          default:
            break;
        }
      },
      [toDetails, orgId, showSaveForm, scheduleData, dispatch, t],
    );

    const moreMenuClick = useCallback((key, _, onNext) => {
      switch (key) {
        case 'batch':
          setBatchMode(value => !value);
          break;
        case 'recycle':
          setBatchMode(false);
          onNext();
          break;
      }
    }, []);

    const recycleMenuClick = useCallback(key => {
      if (key === 'policy') void recycleRef.current?.openPolicy();
      if (key === 'empty') void recycleRef.current?.empty();
    }, []);

    const titles = useMemo(
      () => [
        {
          key: 'list',
          title: t('index.scheduledTaskList'),
          search: true,
          onSearch: listSearch,
          add: {
            items: [
              { key: 'add', text: t('index.newTimedTask') },
              { key: 'folder', text: t('index.addFolder') },
            ],
            callback: toAdd,
          },
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'batch',
                text: batchMode ? '退出批量管理' : '批量管理',
                prefix: <SelectOutlined className="icon" />,
              },
              {
                key: 'recycle',
                text: t('index.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
            ],
            callback: moreMenuClick,
          },
        },
        {
          key: 'recycle',
          title: t('index.recycle'),
          back: true,
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'policy',
                text: '自动清理设置',
                prefix: <SettingOutlined className="icon" />,
              },
              {
                key: 'empty',
                text: '清空回收站',
                prefix: <ClearOutlined className="icon" />,
              },
            ],
            callback: recycleMenuClick,
          },
        },
      ],
      [t, listSearch, toAdd, moreMenuClick, batchMode, recycleMenuClick],
    );

    return (
      <Wrapper
        sliderVisible={sliderVisible}
        className={sliderVisible ? 'close' : ''}
        isDragging={isDragging}
      >
        <SidebarCollapseButton
          collapsed={sliderVisible}
          expandLabel={t('index.open')}
          collapseLabel={t('index.close')}
          onToggle={handleSliderVisible}
        />
        <ListNavWrapper defaultActiveKey="list">
          <ListPane key="list">
            <ListTitle {...titles[0]} />
            {batchMode ? (
              <RecycleBatchManager
                orgId={orgId}
                resourceType="SCHEDULE"
                treeData={scheduleList || []}
                onCompleted={handleBatchCompleted}
                onExit={() => setBatchMode(false)}
              />
            ) : (
              <ScheduleList
                orgId={orgId}
                scheduleId={scheduleId}
                list={scheduleList}
              />
            )}
          </ListPane>
          <ListPane key="recycle">
            <ListTitle {...titles[1]} />
            <RecycleBinManager
              ref={recycleRef}
              orgId={orgId}
              resourceType="SCHEDULE"
            />
          </ListPane>
        </ListNavWrapper>
        <SaveForm
          formProps={{
            labelAlign: 'left',
            labelCol: { offset: 1, span: 8 },
            wrapperCol: { span: 13 },
          }}
          okText={tg('button.save')}
        />
      </Wrapper>
    );
  },
);

const Wrapper = styled.div<{
  sliderVisible: boolean;
  isDragging: boolean;
}>`
  position: relative;
  z-index: ${LEVEL_5};
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  min-height: 0;
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};
  transition: ${p => (!p.isDragging ? 'width 0.3s ease' : 'none')};
  .hidden {
    display: none;
  }
  > ul {
    display: ${p => (p.sliderVisible ? 'none' : 'block')};
  }
  > div {
    display: ${p => (p.sliderVisible ? 'none' : 'flex')};
  }
  &.close {
    position: absolute;
    width: 0 !important;
    height: 100%;
    box-shadow: none;
  }
`;

const ListNavWrapper = styled(ListNav)`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
`;
