package yubi.agent.application;

import yubi.agent.api.ToolInputException;
import yubi.agent.domain.AgentModels.AgentFailure;
import yubi.agent.domain.AgentModels.FailureCategory;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionException;
import yubi.query.api.QueryValidationException;

final class AgentFailureClassifier {

    AgentFailure classify(Throwable failure, boolean canRecover) {
        if (failure instanceof ToolTimeoutException) {
            return new AgentFailure(FailureCategory.TIMEOUT, "TOOL_EXECUTION_TIMEOUT",
                    "只读工具执行超过时间限制", false);
        }
        if (failure instanceof ToolConcurrencyLimitException) {
            return new AgentFailure(FailureCategory.CONCURRENCY_LIMIT, "TOOL_CONCURRENCY_LIMIT",
                    "只读工具并发已达到上限", false);
        }
        if (failure instanceof ToolInputException exception) {
            return new AgentFailure(FailureCategory.VALIDATION, exception.code(),
                    "工具参数不符合只读契约", false);
        }
        if (failure instanceof QueryAccessDeniedException) {
            return new AgentFailure(FailureCategory.ACCESS_DENIED, "QUERY_ACCESS_DENIED",
                    "查询或元数据权限被拒绝", false);
        }
        if (failure instanceof QueryValidationException) {
            return new AgentFailure(FailureCategory.VALIDATION, "QUERY_VALIDATION_FAILED",
                    "查询参数未通过确定性校验", false);
        }
        if (failure instanceof QueryDefinitionException) {
            return new AgentFailure(FailureCategory.DEFINITION, "QUERY_DEFINITION_FAILED",
                    "数据资产定义暂时不可用", canRecover);
        }
        if (failure instanceof QueryExecutionException) {
            return new AgentFailure(FailureCategory.EXECUTION, "QUERY_EXECUTION_FAILED",
                    "只读查询执行失败", canRecover);
        }
        return new AgentFailure(FailureCategory.INTERNAL, "INTERNAL_TOOL_FAILURE",
                "只读工具发生内部失败", false);
    }
}
