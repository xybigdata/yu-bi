package yubi.server.agent.write;

import java.time.Instant;
import java.util.List;

public final class AgentWorkspaceWebModels {

    private AgentWorkspaceWebModels() {
    }

    public record SessionResponse(String sessionId,
                                  Instant expiresAt,
                                  boolean modelRuntimeAvailable,
                                  List<String> writableTools) {
    }

    public record ApprovalResponse(String approvalId,
                                   String sessionId,
                                   String toolName,
                                   String status,
                                   Instant createdAt,
                                   Instant expiresAt,
                                   boolean duplicate,
                                   ApprovalPreview preview,
                                   BusinessChange change,
                                   ApprovalFailure failure) {
    }

    public record ApprovalPreview(String title,
                                  String summary,
                                  List<ApprovalParameter> parameters,
                                  List<ApprovalImpact> impacts) {
    }

    public record ApprovalParameter(String name, String label, String value) {
    }

    public record ApprovalImpact(String resourceType,
                                 String resourceId,
                                 String action,
                                 String description) {
    }

    public record BusinessChange(String changeId,
                                 String resourceType,
                                 String resourceId,
                                 String action,
                                 String finalStatus) {
    }

    public record ApprovalFailure(String code, String message) {
    }

    public record ErrorResponse(boolean success, String code, String message) {
        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(false, code, message);
        }
    }
}
