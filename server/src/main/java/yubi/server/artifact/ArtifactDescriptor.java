package yubi.server.artifact;

public record ArtifactDescriptor(String displayName, String mediaType, String suffix, String source) {

    public ArtifactDescriptor(String displayName, String mediaType, String suffix) {
        this(displayName, mediaType, suffix, "OTHER");
    }

    public ArtifactDescriptor {
        displayName = requireText(displayName, "产物名称不能为空");
        mediaType = requireText(mediaType, "媒体类型不能为空");
        suffix = requireText(suffix, "文件后缀不能为空");
        source = requireText(source, "来源模块不能为空");
        if (!suffix.startsWith(".")) {
            throw new IllegalArgumentException("文件后缀必须以点号开头");
        }
    }

    public String fileName() {
        return displayName.endsWith(suffix) ? displayName : displayName + suffix;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
