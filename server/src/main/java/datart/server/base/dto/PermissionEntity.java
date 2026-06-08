//package datart.server.base.dto;
//
//import datart.security.base.ResourceType;
//import datart.security.base.RoleType;
//import lombok.Data;
//
//import jakarta.validation.constraints.Max;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//
//@NotNull
//@Data
//public class PermissionEntity {
//
//    private String orgId;
//
//    private RoleType roleType;
//
//    @NotBlank
//    private String roleId;
//
//    @NotBlank
//    private ResourceType resourceType;
//
//    @NotBlank
//    private String resourceId;
//
//    @Max(16)
//    @Min(0)
//    private int permission;
//}