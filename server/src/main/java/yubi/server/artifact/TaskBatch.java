package yubi.server.artifact;

import java.util.List;
import java.util.Set;

public record TaskBatch(List<TaskView> tasks, Set<String> missingIds) {

    public TaskBatch {
        tasks = List.copyOf(tasks);
        missingIds = Set.copyOf(missingIds);
    }
}
