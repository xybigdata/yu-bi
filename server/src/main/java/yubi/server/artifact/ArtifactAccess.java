package yubi.server.artifact;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class ArtifactAccess {

    private final String ownerKey;
    private final String executionUser;

    private ArtifactAccess(String ownerKey, String executionUser) {
        this.ownerKey = ownerKey;
        this.executionUser = requireText(executionUser, "执行用户不能为空");
    }

    public static ArtifactAccess authenticated(String userId, String executionUser) {
        return new ArtifactAccess(ownerKey("authenticated", requireText(userId, "用户 ID 不能为空")), executionUser);
    }

    public static ArtifactAccess authenticated(String userId,
                                                String organizationId,
                                                String executionUser) {
        return new ArtifactAccess(ownerKey("authenticated",
                requireText(userId, "用户 ID 不能为空"),
                requireText(organizationId, "组织 ID 不能为空")), executionUser);
    }

    public static ArtifactAccess shared(String clientId, String shareScopeId, String executionUser) {
        return shared(clientId, shareScopeId, executionUser, executionUser);
    }

    public static ArtifactAccess shared(String clientId,
                                        String shareScopeId,
                                        String accessPrincipal,
                                        String executionUser) {
        return new ArtifactAccess(ownerKey("shared",
                requireText(clientId, "客户端 ID 不能为空"),
                requireText(shareScopeId, "分享范围 ID 不能为空"),
                requireText(accessPrincipal, "访问主体不能为空")), executionUser);
    }

    String ownerKey() {
        return ownerKey;
    }

    String executionUser() {
        return executionUser;
    }

    private static String ownerKey(String type, String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(type.getBytes(StandardCharsets.UTF_8));
            for (String part : parts) {
                digest.update((byte) 0);
                digest.update(part.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持 SHA-256", exception);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArtifactAccess access)) {
            return false;
        }
        return ownerKey.equals(access.ownerKey) && executionUser.equals(access.executionUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerKey, executionUser);
    }

    @Override
    public String toString() {
        return "ArtifactAccess[ownerKey=" + ownerKey + "]";
    }
}
