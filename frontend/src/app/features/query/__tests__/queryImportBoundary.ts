import { dirname, join, normalize } from 'node:path';

import ts from 'typescript';

export type QueryThunkImportViolation = {
  importer: string;
  target: string;
  kind: 'default' | 'dynamic' | 'named' | 'namespace' | 're-export' | 'require';
  symbols: string[];
};

export type PageImportViolation = {
  importer: string;
  target: string;
  kind: 'dynamic' | 'import' | 're-export' | 'require';
};

type SourceMap = Record<string, string>;

type Binding = {
  imported?: string;
  namespace?: boolean;
  target: string;
};

type ModuleReference = {
  kind: PageImportViolation['kind'];
  symbols: string[];
  staticKind?: 'default' | 'named' | 'namespace' | 'side-effect';
  target: string;
};

const QUERY_CALLS = new Set([
  'executeQuery',
  'executePublicQuery',
  'fetchChartDataSet',
  'previewQuery',
]);
const PRODUCTION_EXTENSIONS = [
  'ts',
  'tsx',
  'mts',
  'cts',
  'js',
  'jsx',
  'mjs',
  'cjs',
];

// 这些目录编排同一 Dashboard 页面族的行为，不是独立页面实现。
const PAGE_FAMILY_ORCHESTRATORS = ['app/pages/DashBoardPage/actions'];

export function pageOwner(path: string): string | undefined {
  const segments = path.replace(/\\/g, '/').split('/');
  if (segments[0] !== 'app' || segments[1] !== 'pages' || !segments[2]) {
    return undefined;
  }
  if (isPageFamilyOrchestrator(path)) {
    return undefined;
  }
  let ownerIndex = 1;
  for (let index = 3; index < segments.length - 1; index += 1) {
    if (segments[index] === 'pages' && segments[index + 1]) {
      ownerIndex = index;
    }
  }
  return segments.slice(0, ownerIndex + 2).join('/');
}

export function findPageImportViolations(
  sources: SourceMap,
  importsPages: (path: string) => boolean = path =>
    path.startsWith('app/features/') || path.startsWith('app/shared/'),
): PageImportViolation[] {
  const sourceFiles = createSourceFiles(sources);
  const resolveModule = createModuleResolver(sourceFiles);
  const violations: PageImportViolation[] = [];

  for (const [importer, sourceFile] of sourceFiles) {
    if (!importsPages(importer)) {
      continue;
    }
    for (const reference of moduleReferences(
      importer,
      sourceFile,
      resolveModule,
    )) {
      if (
        reference.target === 'app/pages' ||
        reference.target.startsWith('app/pages/')
      ) {
        violations.push({
          importer,
          target: reference.target,
          kind: reference.kind,
        });
      }
    }
  }
  return violations;
}

export function findCrossPageQueryThunkImports(
  sources: SourceMap,
): QueryThunkImportViolation[] {
  const sourceFiles = createSourceFiles(sources);
  const exportCache = new Map<string, Set<string>>();
  const resolving = new Set<string>();
  const resolveModule = createModuleResolver(sourceFiles);

  function bindingsFor(
    path: string,
    sourceFile: ts.SourceFile,
  ): Map<string, Binding> {
    const bindings = new Map<string, Binding>();
    for (const statement of sourceFile.statements) {
      if (
        !ts.isImportDeclaration(statement) ||
        !moduleSpecifierText(statement.moduleSpecifier)
      ) {
        continue;
      }
      const target = resolveModule(
        path,
        moduleSpecifierText(statement.moduleSpecifier)!,
      );
      if (!target || !statement.importClause) {
        continue;
      }
      const { importClause } = statement;
      if (importClause.name) {
        bindings.set(importClause.name.text, { imported: 'default', target });
      }
      const named = importClause.namedBindings;
      if (named && ts.isNamespaceImport(named)) {
        bindings.set(named.name.text, { namespace: true, target });
      } else if (named && ts.isNamedImports(named)) {
        for (const element of named.elements) {
          bindings.set(element.name.text, {
            imported: element.propertyName?.text ?? element.name.text,
            target,
          });
        }
      }
    }
    return bindings;
  }

  function isCreateAsyncThunkCall(
    call: ts.CallExpression,
    bindings: Map<string, Binding>,
  ): boolean {
    if (ts.isIdentifier(call.expression)) {
      const binding = bindings.get(call.expression.text);
      return Boolean(
        binding?.target === '@reduxjs/toolkit' &&
        binding.imported === 'createAsyncThunk',
      );
    }
    if (
      ts.isPropertyAccessExpression(call.expression) &&
      ts.isIdentifier(call.expression.expression)
    ) {
      const binding = bindings.get(call.expression.expression.text);
      return Boolean(
        binding?.target === '@reduxjs/toolkit' &&
        binding.namespace &&
        call.expression.name.text === 'createAsyncThunk',
      );
    }
    return false;
  }

  function callsQueryClient(
    node: ts.Node,
    bindings: Map<string, Binding>,
  ): boolean {
    let found = false;
    function visit(child: ts.Node) {
      if (found) {
        return;
      }
      if (ts.isCallExpression(child)) {
        if (ts.isIdentifier(child.expression)) {
          const binding = bindings.get(child.expression.text);
          found = Boolean(
            binding &&
            !!binding.imported &&
            QUERY_CALLS.has(binding.imported) &&
            isQueryClientModule(binding.target),
          );
        } else if (
          ts.isPropertyAccessExpression(child.expression) &&
          ts.isIdentifier(child.expression.expression)
        ) {
          const binding = bindings.get(child.expression.expression.text);
          found = Boolean(
            binding &&
            binding.namespace &&
            QUERY_CALLS.has(child.expression.name.text) &&
            isQueryClientModule(binding.target),
          );
        }
      }
      ts.forEachChild(child, visit);
    }
    ts.forEachChild(node, visit);
    return found;
  }

  function exportsFor(path: string): Set<string> {
    const cached = exportCache.get(path);
    if (cached) {
      return cached;
    }
    if (resolving.has(path)) {
      return new Set();
    }
    resolving.add(path);
    const sourceFile = sourceFiles.get(path);
    const exports = new Set<string>();
    if (!sourceFile) {
      return exports;
    }
    const bindings = bindingsFor(path, sourceFile);
    const localQueryThunks = new Set<string>();
    for (const statement of sourceFile.statements) {
      if (ts.isVariableStatement(statement)) {
        for (const declaration of statement.declarationList.declarations) {
          if (
            !ts.isIdentifier(declaration.name) ||
            !declaration.initializer ||
            !ts.isCallExpression(declaration.initializer)
          ) {
            continue;
          }
          if (
            isCreateAsyncThunkCall(declaration.initializer, bindings) &&
            callsQueryClient(declaration.initializer, bindings)
          ) {
            localQueryThunks.add(declaration.name.text);
          }
        }
      }
    }
    for (const statement of sourceFile.statements) {
      if (ts.isVariableStatement(statement) && hasExportModifier(statement)) {
        for (const declaration of statement.declarationList.declarations) {
          if (
            ts.isIdentifier(declaration.name) &&
            localQueryThunks.has(declaration.name.text)
          ) {
            exports.add(declaration.name.text);
          }
        }
      }
      if (ts.isExportAssignment(statement) && !statement.isExportEquals) {
        if (
          (ts.isCallExpression(statement.expression) &&
            isCreateAsyncThunkCall(statement.expression, bindings) &&
            callsQueryClient(statement.expression, bindings)) ||
          (ts.isIdentifier(statement.expression) &&
            localQueryThunks.has(statement.expression.text))
        ) {
          exports.add('default');
        }
      }
      if (!ts.isExportDeclaration(statement)) {
        continue;
      }
      const target =
        statement.moduleSpecifier &&
        moduleSpecifierText(statement.moduleSpecifier)
          ? resolveModule(path, moduleSpecifierText(statement.moduleSpecifier)!)
          : undefined;
      if (target) {
        const targetExports = exportsFor(target);
        if (!statement.exportClause) {
          targetExports.forEach(name => exports.add(name));
        } else if (ts.isNamedExports(statement.exportClause)) {
          for (const element of statement.exportClause.elements) {
            const imported = element.propertyName?.text ?? element.name.text;
            if (targetExports.has(imported)) {
              exports.add(element.name.text);
            }
          }
        }
      } else if (
        statement.exportClause &&
        ts.isNamedExports(statement.exportClause)
      ) {
        for (const element of statement.exportClause.elements) {
          const localName = element.propertyName?.text ?? element.name.text;
          if (localQueryThunks.has(localName)) {
            exports.add(element.name.text);
            continue;
          }
          const binding = bindings.get(localName);
          if (
            binding &&
            binding.imported &&
            exportsFor(binding.target).has(binding.imported)
          ) {
            exports.add(element.name.text);
          }
        }
      }
    }
    resolving.delete(path);
    exportCache.set(path, exports);
    return exports;
  }

  function violation(
    importer: string,
    target: string | undefined,
    kind: QueryThunkImportViolation['kind'],
    symbols: string[],
  ): QueryThunkImportViolation | undefined {
    if (!target || pageOwner(importer) === pageOwner(target)) {
      return undefined;
    }
    const queryThunks = exportsFor(target);
    const exposed =
      symbols.length === 0
        ? [...queryThunks]
        : symbols.filter(symbol => queryThunks.has(symbol));
    return exposed.length === 0
      ? undefined
      : { importer, target, kind, symbols: exposed };
  }

  const violations: QueryThunkImportViolation[] = [];
  for (const [importer, sourceFile] of sourceFiles) {
    if (!pageOwner(importer)) {
      continue;
    }
    for (const reference of moduleReferences(
      importer,
      sourceFile,
      resolveModule,
    )) {
      if (
        reference.kind === 'import' &&
        reference.staticKind === 'side-effect'
      ) {
        continue;
      }
      const kind =
        reference.kind === 'import'
          ? reference.staticKind === 'namespace'
            ? 'namespace'
            : reference.staticKind === 'default'
              ? 'default'
              : 'named'
          : reference.kind;
      const item = violation(
        importer,
        reference.target,
        kind,
        reference.symbols,
      );
      if (item) {
        violations.push(item);
      }
    }
  }
  return violations;
}

function isQueryClientModule(target: string): boolean {
  return (
    target === 'app/features/query' ||
    target === 'app/features/query/index.ts' ||
    target === 'app/features/query/client' ||
    target === 'app/features/query/client.ts' ||
    target === 'app/utils/fetch' ||
    target === 'app/utils/fetch.ts'
  );
}

function createSourceFiles(sources: SourceMap): Map<string, ts.SourceFile> {
  return new Map(
    Object.entries(sources).map(([path, source]) => [
      normalizePath(path),
      ts.createSourceFile(path, source, ts.ScriptTarget.Latest, true),
    ]),
  );
}

function createModuleResolver(sourceFiles: Map<string, ts.SourceFile>) {
  return (from: string, specifier: string): string | undefined => {
    const base = specifier.startsWith('.')
      ? normalizePath(join(dirname(from), specifier))
      : normalizePath(specifier);
    const resolved = [
      base,
      ...PRODUCTION_EXTENSIONS.map(extension => `${base}.${extension}`),
      ...PRODUCTION_EXTENSIONS.map(extension => `${base}/index.${extension}`),
    ].find(candidate => sourceFiles.has(candidate));
    return (
      resolved ??
      (base === '@reduxjs/toolkit' ||
      base === 'app/features/query' ||
      base === 'app/features/query/client' ||
      base === 'app/utils/fetch' ||
      base === 'app/pages' ||
      base.startsWith('app/pages/')
        ? base
        : undefined)
    );
  };
}

function moduleReferences(
  importer: string,
  sourceFile: ts.SourceFile,
  resolveModule: (from: string, specifier: string) => string | undefined,
): ModuleReference[] {
  const references: ModuleReference[] = [];
  for (const statement of sourceFile.statements) {
    if (
      ts.isImportDeclaration(statement) &&
      moduleSpecifierText(statement.moduleSpecifier)
    ) {
      const target = resolveModule(
        importer,
        moduleSpecifierText(statement.moduleSpecifier)!,
      );
      if (!target) {
        continue;
      }
      const clause = statement.importClause;
      const named = clause?.namedBindings;
      const staticKind =
        named && ts.isNamedImports(named)
          ? 'named'
          : named && ts.isNamespaceImport(named)
            ? 'namespace'
            : clause?.name
              ? 'default'
              : 'side-effect';
      const symbols =
        named && ts.isNamedImports(named)
          ? named.elements.map(
              element => element.propertyName?.text ?? element.name.text,
            )
          : clause?.name
            ? ['default']
            : [];
      references.push({ kind: 'import', staticKind, symbols, target });
    }
    if (
      ts.isExportDeclaration(statement) &&
      statement.moduleSpecifier &&
      moduleSpecifierText(statement.moduleSpecifier)
    ) {
      const target = resolveModule(
        importer,
        moduleSpecifierText(statement.moduleSpecifier)!,
      );
      if (!target) {
        continue;
      }
      references.push({
        kind: 're-export',
        symbols:
          statement.exportClause && ts.isNamedExports(statement.exportClause)
            ? statement.exportClause.elements.map(
                element => element.propertyName?.text ?? element.name.text,
              )
            : [],
        target,
      });
    }
  }
  function visit(node: ts.Node) {
    if (ts.isCallExpression(node)) {
      const specifier = node.arguments[0];
      const kind =
        node.expression.kind === ts.SyntaxKind.ImportKeyword
          ? 'dynamic'
          : ts.isIdentifier(node.expression) &&
              node.expression.text === 'require'
            ? 'require'
            : undefined;
      const modulePath = moduleSpecifierText(specifier);
      if (kind && modulePath) {
        const target = resolveModule(importer, modulePath);
        if (target) {
          references.push({ kind, symbols: [], target });
        }
      }
    }
    ts.forEachChild(node, visit);
  }
  ts.forEachChild(sourceFile, visit);
  return references;
}

function isPageFamilyOrchestrator(path: string): boolean {
  const normalized = normalizePath(path);
  return PAGE_FAMILY_ORCHESTRATORS.some(
    root => normalized === root || normalized.startsWith(`${root}/`),
  );
}

function normalizePath(path: string): string {
  return normalize(path).replace(/\\/g, '/').replace(/^\.\//, '');
}

function hasExportModifier(node: ts.Node): boolean {
  return (
    (ts.getCombinedModifierFlags(node as ts.Declaration) &
      ts.ModifierFlags.Export) !==
    0
  );
}

function moduleSpecifierText(node: ts.Node | undefined): string | undefined {
  return node &&
    (ts.isStringLiteral(node) || ts.isNoSubstitutionTemplateLiteral(node))
    ? node.text
    : undefined;
}
