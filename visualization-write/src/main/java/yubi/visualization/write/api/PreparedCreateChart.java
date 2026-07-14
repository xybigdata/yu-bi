package yubi.visualization.write.api;

public record PreparedCreateChart(CreateChartCommand command,
                                  WritePreview preview,
                                  PreparedWriteBinding binding) {
}
