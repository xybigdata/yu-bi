package yubi.server.base.params;

import yubi.security.base.ResourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorypageUpdateParam extends VizUpdateParam {

    private String relId;

    private ResourceType relType;

    private String config;

}