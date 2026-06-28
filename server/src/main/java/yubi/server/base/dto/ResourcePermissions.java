package yubi.server.base.dto;

import yubi.security.base.PermissionInfo;
import yubi.security.base.ResourceType;
import lombok.Data;

import java.util.List;

@Data
public class ResourcePermissions {

    private String orgId;

    private ResourceType resourceType;

    private String resourceId;

    private List<PermissionInfo> userPermissions;

    private List<PermissionInfo> rolePermissions;

}
