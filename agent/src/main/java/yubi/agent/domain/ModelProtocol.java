package yubi.agent.domain;

import yubi.query.api.MetadataToolSchema;

import java.util.List;

import static yubi.agent.domain.AgentModels.AgentStep;
import static yubi.agent.domain.StructuredValue.ObjectValue;

public final class ModelProtocol {

    private ModelProtocol() {
    }

    public sealed interface ModelDecision permits ToolCall, FinalAnswer {
    }

    public record ToolCall(String toolName, ObjectValue arguments) implements ModelDecision {
    }

    public record FinalAnswer(String answer) implements ModelDecision {
    }

    /** 不包含主体、组织、角色、权限或分享身份。 */
    public record ModelTurn(String userMessage,
                            int stepIndex,
                            List<MetadataToolSchema> tools,
                            List<AgentStep> history) {
        public ModelTurn {
            tools = tools == null ? List.of() : List.copyOf(tools);
            history = history == null ? List.of() : List.copyOf(history);
        }
    }
}
