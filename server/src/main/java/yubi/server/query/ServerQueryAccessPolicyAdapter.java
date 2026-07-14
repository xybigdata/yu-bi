package yubi.server.query;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.base.consts.Const;
import yubi.core.common.RequestContext;
import yubi.core.entity.RelSubjectColumns;
import yubi.core.entity.Source;
import yubi.core.entity.View;
import yubi.core.mappers.ext.RelSubjectColumnsMapperExt;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.AccessDecision;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.SourceDefinition;
import yubi.query.port.QueryAccessPolicyPort;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.service.SourceService;
import yubi.server.service.ViewService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ServerQueryAccessPolicyAdapter implements QueryAccessPolicyPort {

    private final ViewService viewService;
    private final SourceService sourceService;
    private final RelSubjectColumnsMapperExt columnMapper;
    private final YuBiSecurityManager securityManager;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public ServerQueryAccessPolicyAdapter(ViewService viewService,
                                          SourceService sourceService,
                                          RelSubjectColumnsMapperExt columnMapper,
                                          YuBiSecurityManager securityManager) {
        this.viewService = viewService;
        this.sourceService = sourceService;
        this.columnMapper = columnMapper;
        this.securityManager = securityManager;
    }

    @Override
    public AccessDecision authorize(Definition definition, QueryExecutionContext context) {
        View view = new View();
        view.setId(definition.viewId());
        view.setOrgId(definition.organizationId());
        view.setName(definition.viewName());
        view.setParentId(definition.parentId());
        if (context.channel() != Channel.SHARED) {
            viewService.requirePermission(view, Const.READ);
        }
        boolean owner = securityManager.isOrgOwner(definition.organizationId());
        boolean scriptVisible = hasManagePermission(view);
        RequestContext.setScriptPermission(scriptVisible);
        Set<ColumnSelection> columns = owner
                ? Set.of(new ColumnSelection(null, List.of("*")))
                : allowedColumns(view.getId(), context.subjectId());
        return new AccessDecision(owner, columns, scriptVisible);
    }

    @Override
    public boolean authorizePreview(SourceDefinition sourceDefinition, QueryExecutionContext context) {
        Source source = new Source();
        source.setId(sourceDefinition.id());
        source.setOrgId(sourceDefinition.organizationId());
        source.setName(sourceDefinition.name());
        source.setParentId(sourceDefinition.parentId());
        sourceService.requirePermission(source, Const.READ);
        return securityManager.isOrgOwner(sourceDefinition.organizationId());
    }

    private boolean hasManagePermission(View view) {
        try {
            viewService.requirePermission(view, Const.MANAGE);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Set<ColumnSelection> allowedColumns(String viewId, String userId) {
        try {
            Set<ColumnSelection> columns = new HashSet<>();
            for (RelSubjectColumns permission : columnMapper.listByUser(viewId, userId)) {
                List<String> values = objectMapper.readerForListOf(String.class)
                        .readValue(permission.getColumnPermission());
                for (String value : values) {
                    if (value != null && !value.isBlank()) {
                        columns.add(new ColumnSelection(null, List.of(value.split("\\."))));
                    }
                }
            }
            return columns;
        } catch (Exception ex) {
            throw new IllegalStateException("列权限无法解析", ex);
        }
    }
}
