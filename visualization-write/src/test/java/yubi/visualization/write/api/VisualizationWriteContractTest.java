package yubi.visualization.write.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisualizationWriteContractTest {

    @Test
    void publicCommandsAndTrustedContextMustExposeOnlyTheDeclaredFields() {
        assertEquals(Set.of("subjectId", "organizationId", "sessionId", "requestId", "correlationId"),
                components(VisualizationWriteContext.class));
        assertEquals(Set.of("name", "viewId", "parentId", "description"),
                components(CreateChartCommand.class));
        assertEquals(Set.of("dashboardId", "newName"), components(RenameDashboardCommand.class));
        assertEquals(Set.of("command", "preview", "binding"), components(PreparedCreateChart.class));
        assertEquals(Set.of("command", "preview", "binding", "stateFingerprint"),
                components(PreparedRenameDashboard.class));
    }

    @Test
    void previewMustOwnImmutableOrderedCopies() {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put("name", "chart");
        parameters.put("viewId", "view-1");
        java.util.ArrayList<String> impacts = new java.util.ArrayList<>(List.of("create"));

        WritePreview preview = new WritePreview("title", "summary", parameters, impacts);
        parameters.clear();
        impacts.clear();

        assertEquals(List.of("name", "viewId"), preview.safeParameters().keySet().stream().toList());
        assertEquals(List.of("create"), preview.impacts());
        assertThrows(UnsupportedOperationException.class, preview.safeParameters()::clear);
        assertThrows(UnsupportedOperationException.class, preview.impacts()::clear);
    }

    @Test
    void stableFailureMustExposeFiniteCategoryWithoutCauseOrCustomMessage() {
        for (VisualizationWriteFailureCode code : VisualizationWriteFailureCode.values()) {
            VisualizationWriteException failure = new VisualizationWriteException(code);
            assertEquals(code, failure.code());
            assertEquals(code.category(), failure.category());
            assertEquals(code.safeMessage(), failure.getMessage());
            assertNull(failure.getCause());
            assertFalse(failure.toString().contains("jdbc:"));
        }
    }

    private Set<String> components(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
