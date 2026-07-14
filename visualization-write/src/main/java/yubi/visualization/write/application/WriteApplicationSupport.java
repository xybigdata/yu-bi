package yubi.visualization.write.application;

import yubi.visualization.write.api.CreateChartCommand;
import yubi.visualization.write.api.PreparedWriteBinding;
import yubi.visualization.write.api.RenameDashboardCommand;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.api.VisualizationWriteException;
import yubi.visualization.write.api.VisualizationWriteFailureCode;

import java.util.Objects;
import java.util.function.Supplier;

final class WriteApplicationSupport {

    private static final int MAXIMUM_NAME_LENGTH = 255;
    private static final int MAXIMUM_ID_LENGTH = 128;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 255;
    private static final int MAXIMUM_CONTEXT_VALUE_LENGTH = 256;
    private static final int MAXIMUM_FINGERPRINT_LENGTH = 512;

    private WriteApplicationSupport() {
    }

    static VisualizationWriteContext requireContext(VisualizationWriteContext context) {
        if (context == null
                || invalidRequired(context.subjectId(), MAXIMUM_CONTEXT_VALUE_LENGTH)
                || invalidRequired(context.organizationId(), MAXIMUM_CONTEXT_VALUE_LENGTH)
                || invalidRequired(context.sessionId(), MAXIMUM_CONTEXT_VALUE_LENGTH)
                || invalidRequired(context.requestId(), MAXIMUM_CONTEXT_VALUE_LENGTH)
                || invalidRequired(context.correlationId(), MAXIMUM_CONTEXT_VALUE_LENGTH)) {
            throw failure(VisualizationWriteFailureCode.INVALID_TRUSTED_CONTEXT);
        }
        return context;
    }

    static CreateChartCommand normalize(CreateChartCommand command) {
        if (command == null) {
            throw failure(VisualizationWriteFailureCode.INVALID_CREATE_CHART_COMMAND);
        }
        String name = required(command.name(), MAXIMUM_NAME_LENGTH,
                VisualizationWriteFailureCode.INVALID_CREATE_CHART_COMMAND);
        String viewId = identifier(command.viewId(), VisualizationWriteFailureCode.INVALID_CREATE_CHART_COMMAND);
        String parentId = optionalIdentifier(command.parentId(),
                VisualizationWriteFailureCode.INVALID_CREATE_CHART_COMMAND);
        String description = optional(command.description(), MAXIMUM_DESCRIPTION_LENGTH,
                VisualizationWriteFailureCode.INVALID_CREATE_CHART_COMMAND);
        return new CreateChartCommand(name, viewId, parentId, description);
    }

    static RenameDashboardCommand normalize(RenameDashboardCommand command) {
        if (command == null) {
            throw failure(VisualizationWriteFailureCode.INVALID_RENAME_DASHBOARD_COMMAND);
        }
        String dashboardId = identifier(command.dashboardId(),
                VisualizationWriteFailureCode.INVALID_RENAME_DASHBOARD_COMMAND);
        String newName = required(command.newName(), MAXIMUM_NAME_LENGTH,
                VisualizationWriteFailureCode.INVALID_RENAME_DASHBOARD_COMMAND);
        return new RenameDashboardCommand(dashboardId, newName);
    }

    static PreparedWriteBinding binding(VisualizationWriteContext context) {
        return new PreparedWriteBinding(context.subjectId(), context.organizationId(), context.sessionId(),
                context.requestId(), context.correlationId());
    }

    static boolean bindingMatches(PreparedWriteBinding binding, VisualizationWriteContext context) {
        return binding != null
                && Objects.equals(binding.subjectId(), context.subjectId())
                && Objects.equals(binding.organizationId(), context.organizationId())
                && Objects.equals(binding.sessionId(), context.sessionId())
                && !invalidRequired(binding.originatingRequestId(), MAXIMUM_CONTEXT_VALUE_LENGTH)
                && !invalidRequired(binding.originatingCorrelationId(), MAXIMUM_CONTEXT_VALUE_LENGTH);
    }

    static boolean validFingerprint(String fingerprint) {
        return !invalidRequired(fingerprint, MAXIMUM_FINGERPRINT_LENGTH);
    }

    static boolean validIdentifier(String value) {
        return !invalidIdentifier(value);
    }

    static boolean validName(String value) {
        return !invalidRequired(value, MAXIMUM_NAME_LENGTH) && value.equals(value.trim());
    }

    static <T> T portCall(Supplier<T> operation, VisualizationWriteFailureCode failureCode) {
        T result;
        try {
            result = operation.get();
        } catch (RuntimeException ignored) {
            throw failure(failureCode);
        }
        if (result == null) {
            throw failure(failureCode);
        }
        return result;
    }

    static VisualizationWriteException failure(VisualizationWriteFailureCode code) {
        return new VisualizationWriteException(code);
    }

    private static String required(String value, int maximumLength, VisualizationWriteFailureCode code) {
        if (invalidRequired(value, maximumLength)) {
            throw failure(code);
        }
        return value.trim();
    }

    private static String identifier(String value, VisualizationWriteFailureCode code) {
        String normalized = required(value, MAXIMUM_ID_LENGTH, code);
        if (invalidIdentifier(normalized)) {
            throw failure(code);
        }
        return normalized;
    }

    private static String optionalIdentifier(String value, VisualizationWriteFailureCode code) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return identifier(value, code);
    }

    private static String optional(String value, int maximumLength, VisualizationWriteFailureCode code) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, maximumLength, code);
    }

    private static boolean invalidRequired(String value, int maximumLength) {
        return value == null || value.isBlank() || value.length() > maximumLength || hasUnsafeControl(value);
    }

    private static boolean invalidIdentifier(String value) {
        if (invalidRequired(value, MAXIMUM_ID_LENGTH) || !value.equals(value.trim())) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnsafeControl(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current) && current != '\n' && current != '\r' && current != '\t') {
                return true;
            }
        }
        return false;
    }
}
