type SqlFormatterModule = typeof import('sql-formatter');

let sqlFormatterPromise: Promise<SqlFormatterModule> | null = null;

export function loadSqlFormatter() {
  if (!sqlFormatterPromise) {
    sqlFormatterPromise = import('sql-formatter');
  }

  return sqlFormatterPromise;
}
