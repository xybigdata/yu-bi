export type SqlFormatterModule = typeof import('sql-formatter');

let sqlFormatterPromise: Promise<SqlFormatterModule> | null = null;
let sqlFormatterLoader: () => Promise<SqlFormatterModule> = () =>
  import('sql-formatter');

export function loadSqlFormatter() {
  if (!sqlFormatterPromise) {
    sqlFormatterPromise = sqlFormatterLoader().catch(error => {
      sqlFormatterPromise = null;
      throw error;
    });
  }

  return sqlFormatterPromise;
}

export function __setSqlFormatterLoaderForTest(
  loader: () => Promise<SqlFormatterModule>,
) {
  sqlFormatterLoader = loader;
  sqlFormatterPromise = null;
}

export function __resetSqlFormatterLoaderForTest() {
  sqlFormatterLoader = () => import('sql-formatter');
  sqlFormatterPromise = null;
}
