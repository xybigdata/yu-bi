package yubi.server.base.dto;

import yubi.core.entity.Storyboard;
import yubi.core.entity.Storypage;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoryboardDetail extends Storyboard {

    List<Storypage> storypages;

    private boolean download;

}
