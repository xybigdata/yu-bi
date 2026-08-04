package yubi.server.artifact;

import java.util.List;

public record TaskPage(List<TaskView> tasks, Integer nextOffset) {
}
