import { View } from '../../types/View';
import { APP_VERSION_BETA_4 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';
import { migrateViewConfig as migrateViewDetailConfig } from './migrationViewDetailConfig';
import beginViewModelMigration from './migrationViewModelConfig';

type ViewConfigMigrationTarget = Pick<View, 'type'> & {
  version?: string;
};

export const beta4 = (view?: ViewConfigMigrationTarget | null) => {
  if (!view) {
    return view;
  }

  try {
    if (!view.type) {
      view.type = 'SQL';
    }

    return view;
  } catch (error) {
    console.error('Migration view Errors | beta.4 | ', error);
    return view;
  }
};

const beta4Task = (view?: ViewConfigMigrationTarget) => {
  const result = beta4(view);
  return result ?? view;
};

const migrationViewConfig = (view: View): View => {
  if (!view) {
    return view;
  }

  const event2 = new MigrationEvent<ViewConfigMigrationTarget>(
    APP_VERSION_BETA_4,
    beta4Task,
  );
  const dispatcher =
    new MigrationEventDispatcher<ViewConfigMigrationTarget>(event2);
  const result = dispatcher.process(view);

  return result as View;
};

export const migrateView = (view?: View | null): View | null | undefined => {
  if (!view) {
    return view;
  }

  const migratedView = migrationViewConfig(view);
  if (migratedView.config) {
    migratedView.config = migrateViewDetailConfig(migratedView.config);
  }
  if (migratedView.model) {
    migratedView.model = beginViewModelMigration(
      migratedView.model,
      migratedView.type,
    );
  }

  return migratedView;
};

export default migrationViewConfig;
