import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
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
