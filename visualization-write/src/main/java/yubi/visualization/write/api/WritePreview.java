package yubi.visualization.write.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WritePreview(String title,
                           String summary,
                           Map<String, String> safeParameters,
                           List<String> impacts) {
    public WritePreview {
        safeParameters = safeParameters == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(safeParameters));
        impacts = impacts == null ? List.of() : List.copyOf(impacts);
    }
}
