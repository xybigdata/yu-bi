import { loadSqlFormatter } from './sqlFormatterRuntime';

export async function formatSqlScript(script: string) {
  const { format } = await loadSqlFormatter();
  return format(script, {
    denseOperators: true,
    logicalOperatorNewline: 'before',
  });
}
