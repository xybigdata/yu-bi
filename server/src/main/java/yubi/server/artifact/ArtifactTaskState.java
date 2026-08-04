package yubi.server.artifact;

public enum ArtifactTaskState {
    QUEUED,
    RUNNING,
    READY,
    FAILED,
    TIMED_OUT;

    public boolean isTerminal() {
        return this == READY || this == FAILED || this == TIMED_OUT;
    }
}
