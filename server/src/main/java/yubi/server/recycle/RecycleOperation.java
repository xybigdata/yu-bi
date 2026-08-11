package yubi.server.recycle;

public enum RecycleOperation {
    MOVE_TO_RECYCLE,
    RESTORE,
    PERMANENT_DELETE,
    EMPTY,
    AUTO_CLEANUP,
    UPDATE_POLICY,
    UNDO
}
