package yubi.server.agent;

import java.util.List;
import java.util.Map;

public final class AgentWorkspaceRunWebModels {

    private AgentWorkspaceRunWebModels() {
    }

    public record RunResponse(String runId,
                              String status,
                              List<RunPlanStep> plan,
                              List<RunStep> steps,
                              String finalAnswer,
                              RunFailure failure,
                              RunResultSize resultSize) {
    }

    public record RunPlanStep(int index,
                              String kind,
                              String toolName,
                              String status) {
    }

    public record RunStep(int index,
                          String kind,
                          String toolName,
                          String status,
                          long durationMillis,
                          RunDataResult result,
                          RunFailure failure) {
    }

    public record RunDataResult(Map<String, Object> data,
                                RunResultSize size) {
    }

    public record RunResultSize(int observedItems,
                                int returnedItems,
                                long observedBytes,
                                long returnedBytes,
                                int maximumItems,
                                long maximumBytes,
                                boolean truncated) {
    }

    public record RunFailure(String category,
                             String code,
                             String message,
                             boolean recoverable) {
    }
}
