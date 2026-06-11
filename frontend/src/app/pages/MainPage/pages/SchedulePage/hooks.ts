import { useLocation } from 'app/routerCompat';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { useSelector } from 'react-redux';
import { matchPath } from 'react-router-dom';

const SCHEDULE_DETAIL_PATH = '/organizations/:orgId/schedules/:scheduleId';
const SCHEDULE_LIST_PATH = '/organizations/:orgId/schedules';

type ScheduleRouteParams = {
  orgId?: string;
  scheduleId?: string;
};

export const useScheduleRouteParams = () => {
  const location = useLocation();
  const currentOrgId = useSelector(selectOrgId);
  const detailMatch = matchPath(
    { path: SCHEDULE_DETAIL_PATH, end: true },
    location.pathname,
  );
  const listMatch = matchPath(
    { path: SCHEDULE_LIST_PATH, end: true },
    location.pathname,
  );
  const params: ScheduleRouteParams =
    detailMatch?.params || listMatch?.params || {};

  return {
    orgId: (params.orgId || currentOrgId) as string,
    scheduleId: params.scheduleId as string | undefined,
  };
};

export const useToScheduleDetails = () => {
  const navigate = useCompatNavigate();
  return {
    toDetails: (orgId: string, scheduleId?: string) => {
      if (scheduleId) {
        navigate.push(`/organizations/${orgId}/schedules/${scheduleId}`);
      } else {
        navigate.replace(`/organizations/${orgId}/schedules`);
      }
    },
  };
};
