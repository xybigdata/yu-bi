package yubi.server.query;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.base.consts.Const;
import yubi.core.entity.RelSubjectColumns;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.core.mappers.ext.RelSubjectColumnsMapperExt;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.AssetReference;
import yubi.query.domain.QueryMetadataModels.MetadataAccessDecision;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.port.QueryMetadataAccessPolicyPort;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.service.ViewService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ServerQueryMetadataAccessPolicyAdapter implements QueryMetadataAccessPolicyPort {

    private final ViewService viewService;
    private final RelSubjectColumnsMapperExt columnMapper;
    private final YuBiSecurityManager securityManager;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public ServerQueryMetadataAccessPolicyAdapter(ViewService viewService,
                                                  RelSubjectColumnsMapperExt columnMapper,
                                                  YuBiSecurityManager securityManager) {
        this.viewService = viewService;
        this.columnMapper = columnMapper;
        this.securityManager = securityManager;
    }

    @Override
    public void validateContext(QueryExecutionContext context) {
        User currentUser = securityManager.getCurrentUser();
        if (currentUser == null || !context.subjectId().equals(currentUser.getId())) {
            throw new SecurityException("当前安全主体与元数据上下文不一致");
        }
    }

    @Override
    public MetadataAccessDecision authorize(AssetReference asset, QueryExecutionContext context) {
        validateContext(context);
        View view = view(asset);
        viewService.requirePermission(view, Const.READ);
        boolean owner = securityManager.isOrgOwner(asset.organizationId());
        Set<ColumnSelection> columns = owner
                ? Set.of(new ColumnSelection(null, List.of("*")))
                : allowedColumns(asset.id(), context.subjectId());
        return new MetadataAccessDecision(owner, columns, hasManagePermission(view));
    }

    private View view(AssetReference asset) {
        View view = new View();
        view.setId(asset.id());
        view.setOrgId(asset.organizationId());
        view.setName(asset.name());
        view.setParentId(asset.parentId());
        view.setSourceId(asset.sourceId());
        return view;
    }

    private boolean hasManagePermission(View view) {
        try {
            viewService.requirePermission(view, Const.MANAGE);
            return true;
        } catch (RuntimeException ignored) {
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
        } catch (Exception exception) {
            throw new IllegalStateException("列权限无法解析", exception);
        }
    }
}
