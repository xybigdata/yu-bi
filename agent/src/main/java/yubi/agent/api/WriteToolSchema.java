package yubi.agent.api;

import yubi.query.api.MetadataToolSchema.Schema;

import java.util.Objects;

/** 与只读工具注册表隔离的受控写工具协议。 */
public record WriteToolSchema(String name,
                              String description,
                              Schema inputSchema,
                              Schema previewSchema,
                              Effect effect,
                              boolean requiresApproval) {

    public WriteToolSchema {
        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("写工具 Schema 标识不完整");
        }
        Objects.requireNonNull(inputSchema, "inputSchema");
        Objects.requireNonNull(previewSchema, "previewSchema");
        Objects.requireNonNull(effect, "effect");
    }

    public enum Effect {
        READ,
        WRITE
    }
}
