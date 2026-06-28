package yubi.server.base.dto;

import yubi.core.entity.BaseEntity;
import yubi.core.entity.Storypage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StorypageDetail extends Storypage {

    BaseEntity vizDetail;

}
