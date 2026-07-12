import { readdirSync, readFileSync } from 'node:fs';
import { relative, resolve } from 'node:path';
import ts from 'typescript';
import { describe, expect, test } from 'vitest';
import { AGENT_READ_ONLY_TOOLS, AGENT_WRITABLE_TOOLS } from '../types';
import { findPageImportViolations } from '../../query/__tests__/queryImportBoundary';

const sourceRoot = resolve(import.meta.dirname, '../../../..');
const productionExtension = /\.(?:[cm]?[jt]s|[jt]sx)$/;
const testFile = /\.(?:test|spec)\.(?:[cm]?[jt]s|[jt]sx)$/;
const testDirectory = /^(?:__tests__|tests?)$/;

function productionSources(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      return testDirectory.test(entry.name) ? [] : productionSources(path);
    }
    if (!productionExtension.test(entry.name) || testFile.test(entry.name)) {
      return [];
    }
    return [relative(sourceRoot, path).replaceAll('\\', '/')];
  });
}

const allProductionSources = productionSources(resolve(sourceRoot, 'app'));
const agentSources = allProductionSources.filter(
  file =>
    file.startsWith('app/features/agent/') ||
    file.startsWith('app/pages/MainPage/pages/AgentPage/'),
);
const agentPageSources = agentSources.filter(file =>
  file.startsWith('app/pages/MainPage/pages/AgentPage/'),
);
const agentClientFile = 'app/features/agent/client.ts';

const readSource = (file: string) =>
  readFileSync(resolve(sourceRoot, file), 'utf8');

const agentClientSource = readSource(agentClientFile);
const agentClientAst = ts.createSourceFile(
  agentClientFile,
  agentClientSource,
  ts.ScriptTarget.Latest,
  true,
);

const hasExportModifier = (node: ts.Node) =>
  ts.canHaveModifiers(node) &&
  ts
    .getModifiers(node)
    ?.some(modifier => modifier.kind === ts.SyntaxKind.ExportKeyword);

function exportedClientValues(): string[] {
  return agentClientAst.statements
    .flatMap(statement => {
      if (ts.isExportDeclaration(statement) && !statement.isTypeOnly) {
        if (!statement.exportClause) {
          return ['*'];
        }
        if (ts.isNamedExports(statement.exportClause)) {
          return statement.exportClause.elements.flatMap(element =>
            element.isTypeOnly ? [] : [element.name.text],
          );
        }
      }
      if (ts.isExportAssignment(statement)) {
        return ['default'];
      }
      if (!hasExportModifier(statement)) {
        return [];
      }
      if (ts.isFunctionDeclaration(statement) && statement.name) {
        return [statement.name.text];
      }
      if (
        (ts.isClassDeclaration(statement) || ts.isEnumDeclaration(statement)) &&
        statement.name
      ) {
        return [statement.name.text];
      }
      if (ts.isVariableStatement(statement)) {
        return statement.declarationList.declarations.flatMap(declaration =>
          ts.isIdentifier(declaration.name) ? [declaration.name.text] : [],
        );
      }
      return [];
    })
    .sort();
}

interface AgentRequestSpec {
  owner: string;
  method: string;
  url: string;
}

const compactNodeText = (node: ts.Node) =>
  node.getText(agentClientAst).replaceAll(/\s/g, '').replaceAll(/,\)/g, ')');

function containingCallableName(node: ts.Node): string | undefined {
  let current: ts.Node | undefined = node.parent;
  while (current) {
    if (ts.isFunctionDeclaration(current) && current.name) {
      return current.name.text;
    }
    if (
      (ts.isArrowFunction(current) || ts.isFunctionExpression(current)) &&
      ts.isVariableDeclaration(current.parent) &&
      ts.isIdentifier(current.parent.name)
    ) {
      return current.parent.name.text;
    }
    current = current.parent;
  }
  return undefined;
}

function requestProperty(
  config: ts.ObjectLiteralExpression,
  name: 'method' | 'url',
): ts.Expression | undefined {
  const property = config.properties.find(
    candidate =>
      ts.isPropertyAssignment(candidate) &&
      ((ts.isIdentifier(candidate.name) && candidate.name.text === name) ||
        (ts.isStringLiteralLike(candidate.name) &&
          candidate.name.text === name)),
  );
  return property && ts.isPropertyAssignment(property)
    ? property.initializer
    : undefined;
}

function agentRequestSpecs(): AgentRequestSpec[] {
  const specs: AgentRequestSpec[] = [];
  const visit = (node: ts.Node) => {
    if (
      ts.isCallExpression(node) &&
      ts.isIdentifier(node.expression) &&
      node.expression.text === 'request2'
    ) {
      const config = node.arguments[0];
      const owner = containingCallableName(node);
      if (!config || !ts.isObjectLiteralExpression(config) || !owner) {
        specs.push({ owner: owner || '<unknown>', method: '', url: '' });
      } else {
        const method = requestProperty(config, 'method');
        const url = requestProperty(config, 'url');
        specs.push({
          owner,
          method: method && ts.isStringLiteralLike(method) ? method.text : '',
          url: url ? compactNodeText(url) : '',
        });
      }
    }
    ts.forEachChild(node, visit);
  };
  visit(agentClientAst);
  return specs.sort((left, right) => left.owner.localeCompare(right.owner));
}

function variableInitializer(name: string): ts.Expression | undefined {
  for (const statement of agentClientAst.statements) {
    if (!ts.isVariableStatement(statement)) {
      continue;
    }
    const declaration = statement.declarationList.declarations.find(
      candidate =>
        ts.isIdentifier(candidate.name) && candidate.name.text === name,
    );
    if (declaration?.initializer) {
      return declaration.initializer;
    }
  }
  return undefined;
}

function approvalDecision(name: string): string | undefined {
  const initializer = variableInitializer(name);
  if (!initializer || !ts.isArrowFunction(initializer)) {
    return undefined;
  }
  const body = initializer.body;
  if (
    !ts.isCallExpression(body) ||
    !ts.isIdentifier(body.expression) ||
    body.expression.text !== 'decideAgentApproval' ||
    body.arguments.length !== 4
  ) {
    return undefined;
  }
  const decision = body.arguments[3];
  return ts.isStringLiteralLike(decision) ? decision.text : undefined;
}

function moduleSpecifiers(file: string): string[] {
  const sourceFile = ts.createSourceFile(
    file,
    readSource(file),
    ts.ScriptTarget.Latest,
    true,
  );
  const values: string[] = [];
  const visit = (node: ts.Node) => {
    if (
      (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) &&
      node.moduleSpecifier &&
      ts.isStringLiteralLike(node.moduleSpecifier)
    ) {
      values.push(node.moduleSpecifier.text);
    }
    if (
      ts.isCallExpression(node) &&
      node.arguments.length === 1 &&
      ts.isStringLiteralLike(node.arguments[0]) &&
      (node.expression.kind === ts.SyntaxKind.ImportKeyword ||
        (ts.isIdentifier(node.expression) &&
          node.expression.text === 'require'))
    ) {
      values.push(node.arguments[0].text);
    }
    ts.forEachChild(node, visit);
  };
  visit(sourceFile);
  return values;
}

describe('agent frontend architecture', () => {
  test('写工具允许列表精确且不包含高风险动作', () => {
    expect(AGENT_WRITABLE_TOOLS).toEqual(['create_chart', 'rename_dashboard']);
  });

  test('只读运行入口保持目标 F/G 的精确三工具边界', () => {
    expect(AGENT_READ_ONLY_TOOLS).toEqual([
      'search_data_assets',
      'describe_data_asset',
      'execute_view',
    ]);
    expect(AGENT_READ_ONLY_TOOLS).not.toEqual(
      expect.arrayContaining([...AGENT_WRITABLE_TOOLS]),
    );
  });

  test('Agent REST 地址只存在于 feature client', () => {
    const violations = allProductionSources.filter(file => {
      const hasAgentUrl = /agent\/workspaces\//.test(readSource(file));
      return hasAgentUrl && file !== agentClientFile;
    });
    expect(violations).toEqual([]);
  });

  test('Agent client 运行时导出保持精确请求白名单', () => {
    expect(exportedClientValues()).toEqual(
      [
        'AGENT_SESSION_HEADER',
        'IDEMPOTENCY_KEY_HEADER',
        'approveAgentWrite',
        'createAgentWorkspaceSession',
        'listAgentApprovals',
        'previewAgentWrite',
        'rejectAgentWrite',
        'runAgentReadOnly',
      ].sort(),
    );
  });

  test('Agent client 请求方法与工作区路径保持精确白名单', () => {
    const workspaceUrl = variableInitializer('workspaceUrl');
    expect(workspaceUrl && compactNodeText(workspaceUrl)).toBe(
      '(orgId:string)=>`agent/workspaces/${encodeURIComponent(orgId)}`',
    );
    expect(agentRequestSpecs()).toEqual([
      {
        owner: 'createAgentWorkspaceSession',
        method: 'POST',
        url: '`${workspaceUrl(orgId)}/sessions`',
      },
      {
        owner: 'decideAgentApproval',
        method: 'POST',
        url: '`${workspaceUrl(orgId)}/approvals/${encodeURIComponent(approvalId)}/${decision}`',
      },
      {
        owner: 'listAgentApprovals',
        method: 'GET',
        url: '`${workspaceUrl(orgId)}/approvals`',
      },
      {
        owner: 'previewAgentWrite',
        method: 'POST',
        url: '`${workspaceUrl(orgId)}/writes/previews`',
      },
      {
        owner: 'runAgentReadOnly',
        method: 'POST',
        url: '`${workspaceUrl(orgId)}/runs`',
      },
    ]);
    expect(approvalDecision('approveAgentWrite')).toBe('approve');
    expect(approvalDecision('rejectAgentWrite')).toBe('reject');
  });

  test('Agent 页面不能绕过 feature 或复用旧页面写链路', () => {
    const forbidden = [
      'utils/request',
      'app/pages/ChartWorkbenchPage',
      'app/pages/DashBoardPage',
      'app/pages/MainPage/pages/ViewPage',
      'app/pages/MainPage/pages/VizPage',
    ];
    const violations = agentPageSources.flatMap(file =>
      moduleSpecifiers(file)
        .filter(specifier =>
          forbidden.some(prefix => specifier.startsWith(prefix)),
        )
        .map(specifier => [file, specifier]),
    );
    expect(violations).toEqual([]);
  });

  test('feature 不能反向依赖页面', () => {
    const sources = Object.fromEntries(
      allProductionSources.map(file => [file, readSource(file)]),
    );
    expect(
      findPageImportViolations(sources, file =>
        file.startsWith('app/features/agent/'),
      ),
    ).toEqual([]);
  });

  test('只有 Agent slice thunk 可以导入会产生请求的 feature 函数', () => {
    const mutationNames =
      /(?:approveAgentWrite|createAgentWorkspaceSession|listAgentApprovals|previewAgentWrite|rejectAgentWrite|runAgentReadOnly)/;
    const violations = agentPageSources.filter(file => {
      if (file === 'app/pages/MainPage/pages/AgentPage/slice/thunks.ts') {
        return false;
      }
      return mutationNames.test(readSource(file));
    });
    expect(violations).toEqual([]);
  });

  test('Agent 前端状态不写入浏览器存储或浏览器 URL', () => {
    const forbidden =
      /localStorage|sessionStorage|URLSearchParams|location\.search|history\.pushState/;
    expect(
      agentSources.filter(file => forbidden.test(readSource(file))),
    ).toEqual([]);
  });

  test('Agent 页面不包含旧资源写地址', () => {
    const legacyWriteUrl = /(?:^|[/'"`])\/?(?:viz|views)(?:[/?'"`]|$)/m;
    const legacyImplementation =
      /(?:ChartWorkbenchPage|DashBoardPage|MainPage\/pages\/(?:ViewPage|VizPage)|utils\/fetch)/;
    const controlledSources = agentSources;
    expect(
      controlledSources.filter(file => legacyWriteUrl.test(readSource(file))),
    ).toEqual([]);
    expect(
      controlledSources.flatMap(file =>
        moduleSpecifiers(file)
          .filter(specifier => legacyImplementation.test(specifier))
          .map(specifier => [file, specifier]),
      ),
    ).toEqual([]);
  });

  test('全局拒绝日志不输出完整 action 或 meta 参数', () => {
    const middleware = readSource(
      'utils/@reduxjs/rejectedErrorHandlerMiddleware.ts',
    );
    expect(middleware).not.toMatch(/console\.error\([^;]*,\s*action\s*\)/s);
    expect(middleware).not.toContain('meta.arg');
    expect(middleware).toContain(
      "console.error('Redux 异步操作失败', actionType)",
    );
  });

  test('Agent action 与状态必须接入 DevTools 双侧脱敏投影', () => {
    const store = readSource('redux/configureStore.ts');
    expect(store).toContain('actionSanitizer: sanitizeReduxDevToolsAction');
    expect(store).toContain('stateSanitizer: sanitizeReduxDevToolsState');
    expect(store).toMatch(/devTools:[\s\S]*\? reduxDevToolsOptions\s*: false/);
  });

  test('工作区为桌面与 390px 窄屏提供稳定响应式布局', () => {
    const page = readSource('app/pages/MainPage/pages/AgentPage/index.tsx');
    const approval = readSource(
      'app/pages/MainPage/pages/AgentPage/components/ApprovalCard.tsx',
    );
    expect(page).toContain(
      'grid-template-columns: minmax(0, 1fr) minmax(320px, 420px)',
    );
    expect(page).toMatch(
      /@media \(max-width: 1023px\) \{\s+display: block;\s+overflow: auto;/,
    );
    expect(page).toContain('@media (max-width: 767px)');
    expect(approval).toContain('@media (max-width: 480px)');
    expect(approval).toContain(
      'grid-template-columns: minmax(80px, auto) minmax(0, 1fr)',
    );
    expect(approval).toContain('grid-column: 1 / -1');
  });
});
