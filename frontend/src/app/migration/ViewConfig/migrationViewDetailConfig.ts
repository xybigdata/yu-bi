import { APP_VERSION_BETA_2 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

type ViewDetailConfigMigrationTarget = {
  version?: string;
  expensiveQuery?: boolean;
  concurrencyControl?: boolean;
  concurrencyControlMode?: string;
  cache?: boolean;
  cacheExpires?: number;
};

const parseViewDetailConfig = (
  viewConfig: string,
): ViewDetailConfigMigrationTarget | null | undefined => {
  try {
    return JSON.parse(viewConfig) as ViewDetailConfigMigrationTarget | null;
  } catch (error) {
    return undefined;
  }
};

export const beta2 = (
  viewConfig?: ViewDetailConfigMigrationTarget | null,
) => {
  if (!viewConfig) {
    return viewConfig;
  }

  try {
    viewConfig.expensiveQuery = false;

    return viewConfig;
  } catch (error) {
    console.error('Migration ViewConfig Errors | beta.2 | ', error);
    return viewConfig;
  }
};

const beta2Task = (viewConfig?: ViewDetailConfigMigrationTarget) => {
  const result = beta2(viewConfig);
  return result ?? viewConfig;
};

export const migrateViewConfig = (viewConfig: string) => {
  if (!viewConfig?.trim().length) {
    return viewConfig;
  }

  const config = parseViewDetailConfig(viewConfig);
  if (typeof config === 'undefined') {
    return viewConfig;
  }
  if (config === null) {
    return JSON.stringify(config);
  }

  const event2 = new MigrationEvent<ViewDetailConfigMigrationTarget>(
    APP_VERSION_BETA_2,
    beta2Task,
  );
  const dispatcher = new MigrationEventDispatcher<ViewDetailConfigMigrationTarget>(
    event2,
  );
  const result = dispatcher.process(config);

  return JSON.stringify(result);
};
