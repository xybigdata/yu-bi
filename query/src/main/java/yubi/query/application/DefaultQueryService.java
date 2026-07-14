package yubi.query.application;

import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.PreviewQueryUseCase;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryExecutionException;
import yubi.query.api.QueryResult;
import yubi.query.api.QueryValidationException;
import yubi.query.domain.QueryModels.AccessDecision;
import yubi.query.domain.QueryModels.AuditEvent;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.EngineResult;
import yubi.query.domain.QueryModels.Execution;
import yubi.query.domain.QueryModels.FailureCategory;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.Plan;
import yubi.query.domain.QueryModels.Script;
import yubi.query.domain.QueryModels.SourceDefinition;
import yubi.query.domain.QueryModels.SourceReference;
import yubi.query.domain.QueryModels.Variable;
import yubi.query.domain.QueryModels.VariableType;
import yubi.query.port.QueryAccessPolicyPort;
import yubi.query.port.QueryAuditPort;
import yubi.query.port.QueryDefinitionPort;
import yubi.query.port.QueryEnginePort;
import yubi.query.port.QueryVariablePort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DefaultQueryService implements ExecuteQueryUseCase, PreviewQueryUseCase {

    private static final long DEFAULT_PAGE_NO = 1L;
    private static final long DEFAULT_PAGE_SIZE = 1000L;

    private final QueryDefinitionPort definitionPort;
    private final QueryAccessPolicyPort accessPolicyPort;
    private final QueryVariablePort variablePort;
    private final QueryEnginePort enginePort;
    private final QueryAuditPort auditPort;

    public DefaultQueryService(QueryDefinitionPort definitionPort,
                               QueryAccessPolicyPort accessPolicyPort,
                               QueryVariablePort variablePort,
                               QueryEnginePort enginePort,
                               QueryAuditPort auditPort) {
        this.definitionPort = definitionPort;
        this.accessPolicyPort = accessPolicyPort;
        this.variablePort = variablePort;
        this.enginePort = enginePort;
        this.auditPort = auditPort;
    }

    @Override
    public QueryResult execute(ExecuteQueryCommand command, QueryExecutionContext context) {
        long start = System.nanoTime();
        QueryResult result = null;
        FailureCategory failure = null;
        QueryExecutionContext auditContext = context;
        String resourceId = command == null ? null : command.viewId();
        try {
            validate(command, context);
            Definition definition = loadDefinition(command.viewId());
            auditContext = bindOrganization(context, definition.organizationId());
            AccessDecision access = authorize(definition, auditContext);
            if (command.empty()) {
                result = QueryResult.empty();
                return result;
            }
            List<Variable> variables = resolveVariables(definition, command, auditContext, access.organizationOwner());
            Page page = normalize(command.page());
            Script script = new Script(false, definition.sourceId(), definition.viewId(), definition.script(),
                    definition.scriptType(), variables, definition.schema());
            Execution execution = new Execution(command.keywords(), command.columns(), command.functionColumns(),
                    command.aggregators(), command.filters(), command.groups(), command.orders(),
                    access.allowedColumns(), page, command.concurrencyControl(), command.cache(), command.cacheExpires());
            EngineResult engineResult = executeEngine(new Plan(
                    new SourceReference(definition.sourceId(), definition.organizationId()), script, execution));
            String visibleScript = command.includeScript() && access.scriptVisible() ? engineResult.script() : null;
            result = mapResult(engineResult, visibleScript);
            return result;
        } catch (QueryException ex) {
            failure = ex.category();
            throw ex;
        } catch (RuntimeException ex) {
            QueryExecutionException classified = new QueryExecutionException("查询处理发生未预期失败", ex);
            failure = classified.category();
            throw classified;
        } catch (Error error) {
            failure = FailureCategory.EXECUTION;
            throw error;
        } finally {
            audit(auditContext, resourceId, start, result, failure);
        }
    }

    @Override
    public QueryResult preview(PreviewQueryCommand command, QueryExecutionContext context) {
        long start = System.nanoTime();
        QueryResult result = null;
        FailureCategory failure = null;
        QueryExecutionContext auditContext = context;
        String resourceId = command == null ? null : command.sourceId();
        try {
            validate(command, context);
            SourceDefinition source = loadSource(command.sourceId());
            auditContext = bindOrganization(context, source.organizationId());
            boolean owner = authorizePreview(source, auditContext);
            List<Variable> variables = new ArrayList<>(loadSourceVariables(source, auditContext));
            variables.addAll(command.variables());
            variables = normalizePreviewVariables(variables, owner);
            Script script = new Script(true, source.id(), null, command.script(), command.scriptType(), variables, null);
            Execution execution = new Execution(List.of(), command.columns(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), Set.of(new ColumnSelection(null, List.of("*"))),
                    Page.request(DEFAULT_PAGE_NO, Math.max(command.size(), 1), false), false, false, 0);
            EngineResult engineResult = executeEngine(new Plan(
                    new SourceReference(source.id(), source.organizationId()), script, execution));
            result = mapResult(engineResult, engineResult.script());
            return result;
        } catch (QueryException ex) {
            failure = ex.category();
            throw ex;
        } catch (RuntimeException ex) {
            QueryExecutionException classified = new QueryExecutionException("预览处理发生未预期失败", ex);
            failure = classified.category();
            throw classified;
        } catch (Error error) {
            failure = FailureCategory.EXECUTION;
            throw error;
        } finally {
            audit(auditContext, resourceId, start, result, failure);
        }
    }

    private void validate(ExecuteQueryCommand command, QueryExecutionContext context) {
        if (command == null || isBlank(command.viewId())) {
            throw new QueryValidationException("viewId 不能为空");
        }
        if (context == null) {
            throw new QueryValidationException("查询执行上下文不能为空");
        }
    }

    private void validate(PreviewQueryCommand command, QueryExecutionContext context) {
        if (command == null || isBlank(command.sourceId())) {
            throw new QueryValidationException("sourceId 不能为空");
        }
        if (command.scriptType() == null) {
            throw new QueryValidationException("scriptType 不能为空");
        }
        if (context == null) {
            throw new QueryValidationException("查询执行上下文不能为空");
        }
    }

    private Definition loadDefinition(String viewId) {
        try {
            Definition definition = definitionPort.load(viewId);
            if (definition == null || isBlank(definition.sourceId())) {
                throw new IllegalStateException("查询定义不存在");
            }
            return definition;
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryDefinitionException("无法读取查询定义", ex);
        }
    }

    private SourceDefinition loadSource(String sourceId) {
        try {
            SourceDefinition source = definitionPort.loadSource(sourceId);
            if (source == null) {
                throw new IllegalStateException("数据源定义不存在");
            }
            return source;
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryDefinitionException("无法读取数据源定义", ex);
        }
    }

    private AccessDecision authorize(Definition definition, QueryExecutionContext context) {
        try {
            AccessDecision decision = accessPolicyPort.authorize(definition, context);
            if (decision == null) {
                throw new IllegalStateException("权限端口未返回决策");
            }
            return decision;
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryAccessDeniedException("查询权限校验失败", ex);
        }
    }

    private boolean authorizePreview(SourceDefinition source, QueryExecutionContext context) {
        try {
            return accessPolicyPort.authorizePreview(source, context);
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryAccessDeniedException("预览权限校验失败", ex);
        }
    }

    private List<Variable> resolveVariables(Definition definition,
                                            ExecuteQueryCommand command,
                                            QueryExecutionContext context,
                                            boolean owner) {
        try {
            List<Variable> variables = variablePort.loadForView(
                    definition.viewId(), definition.organizationId(), context);
            return normalizeVariables(variables, command.parameters(), owner);
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryDefinitionException("无法解析查询变量", ex);
        }
    }

    private List<Variable> loadSourceVariables(SourceDefinition source, QueryExecutionContext context) {
        try {
            return variablePort.loadForSource(source.organizationId(), context);
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryDefinitionException("无法解析预览变量", ex);
        }
    }

    private List<Variable> normalizeVariables(List<Variable> source,
                                              java.util.Map<String, Set<String>> overrides,
                                              boolean owner) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(variable -> {
            Variable current = variable;
            if (variable.type() == VariableType.QUERY) {
                if (overrides != null && overrides.containsKey(variable.name())) {
                    current = variable.withValues(overrides.get(variable.name()));
                } else if (variable.expression()) {
                    current = variable.asFragment();
                }
            }
            if (owner && current.type() == VariableType.PERMISSION) {
                current = current.disable();
            }
            return current;
        }).toList();
    }

    private List<Variable> normalizePreviewVariables(List<Variable> source, boolean owner) {
        return source.stream().map(variable -> {
            Variable current = variable.expression() ? variable.asFragment() : variable;
            return owner && current.type() == VariableType.PERMISSION ? current.disable() : current;
        }).toList();
    }

    private EngineResult executeEngine(Plan plan) {
        try {
            EngineResult result = enginePort.execute(plan);
            if (result == null) {
                throw new IllegalStateException("查询引擎未返回结果");
            }
            return result;
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueryExecutionException("查询引擎执行失败", ex);
        }
    }

    private Page normalize(Page page) {
        if (page == null) {
            return Page.request(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE, false);
        }
        long pageNo = page.pageNo() < 1 ? DEFAULT_PAGE_NO : page.pageNo();
        long pageSize = page.pageSize() < 1 ? DEFAULT_PAGE_SIZE : Math.min(page.pageSize(), Integer.MAX_VALUE);
        return Page.request(pageNo, pageSize, page.countTotal());
    }

    private QueryResult mapResult(EngineResult engineResult, String visibleScript) {
        try {
            if (isBlank(engineResult.id())) {
                throw new IllegalStateException("查询结果缺少标识");
            }
            return QueryResult.from(engineResult, visibleScript);
        } catch (RuntimeException ex) {
            throw new QueryExecutionException("查询结果转换失败", ex);
        }
    }

    private QueryExecutionContext bindOrganization(QueryExecutionContext context, String organizationId) {
        if (isBlank(organizationId)) {
            throw new QueryDefinitionException("查询定义缺少组织引用", null);
        }
        if (context.organizationId() != null && !context.organizationId().equals(organizationId)) {
            throw new QueryAccessDeniedException("查询组织与执行上下文不一致", null);
        }
        if (context.organizationId() != null) {
            return context;
        }
        return new QueryExecutionContext(context.channel(), context.subjectId(), organizationId, context.correlationId());
    }

    private void audit(QueryExecutionContext context,
                       String resourceId,
                       long start,
                       QueryResult result,
                       FailureCategory failure) {
        if (context == null) {
            return;
        }
        try {
            int rows = result == null ? 0 : result.rows().size();
            auditPort.record(new AuditEvent(context.channel(), context.subjectId(), context.organizationId(),
                    context.correlationId(), resourceId, (System.nanoTime() - start) / 1_000_000L,
                    rows, failure == null, failure));
        } catch (Exception ignored) {
            // 审计是尽力行为，不能覆盖查询结果或原始异常。
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
