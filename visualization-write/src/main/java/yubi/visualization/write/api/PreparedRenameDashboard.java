package yubi.visualization.write.api;

public record PreparedRenameDashboard(RenameDashboardCommand command,
                                      WritePreview preview,
                                      PreparedWriteBinding binding,
                                      String stateFingerprint) {
}
