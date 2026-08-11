package yubi.server.recycle;

import java.util.Objects;

public record RecycleAccess(String actorId,
                            String organizationId,
                            boolean organizationOwner) {

    public RecycleAccess {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(organizationId, "organizationId");
    }

    public static RecycleAccess authenticated(String actorId,
                                              String organizationId,
                                              boolean organizationOwner) {
        return new RecycleAccess(actorId, organizationId, organizationOwner);
    }
}
