package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.core.entity.User;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.Variable;
import yubi.query.domain.QueryModels.VariableType;
import yubi.query.port.QueryVariablePort;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.VariableValue;
import yubi.server.service.VariableService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ServerQueryVariableAdapter implements QueryVariablePort {

    private final VariableService variableService;
    private final YuBiSecurityManager securityManager;

    public ServerQueryVariableAdapter(VariableService variableService, YuBiSecurityManager securityManager) {
        this.variableService = variableService;
        this.securityManager = securityManager;
    }

    @Override
    public List<Variable> loadForView(String viewId,
                                      String organizationId,
                                      QueryExecutionContext context) {
        List<Variable> variables = new ArrayList<>(loadForSource(organizationId, context));
        variables.addAll(variableService.listViewVarValuesByUser(context.subjectId(), viewId)
                .stream().map(this::convert).toList());
        return variables;
    }

    @Override
    public List<Variable> loadForSource(String organizationId, QueryExecutionContext context) {
        User user = securityManager.getCurrentUser();
        if (!context.subjectId().equals(user.getId())) {
            throw new IllegalStateException("当前安全主体与查询上下文不一致");
        }
        List<Variable> variables = new ArrayList<>();
        variables.add(system("YUBI_USER_NAME", user.getName()));
        variables.add(system("YUBI_USER_EMAIL", user.getEmail()));
        variables.add(system("YUBI_USER_ID", user.getId()));
        variables.add(system("YUBI_USER_USERNAME", user.getUsername()));
        variables.addAll(variableService.listOrgValue(organizationId).stream().map(this::convert).toList());
        return variables;
    }

    private Variable system(String name, String value) {
        return new Variable(name, VariableType.PERMISSION, ValueType.STRING,
                value == null ? Set.of() : Set.of(value), false, false, null);
    }

    private Variable convert(VariableValue value) {
        return new Variable(value.getName(), VariableType.valueOf(value.getType()),
                ValueType.valueOf(value.getValueType()), value.getValues(), value.isExpression(), false, null);
    }
}
