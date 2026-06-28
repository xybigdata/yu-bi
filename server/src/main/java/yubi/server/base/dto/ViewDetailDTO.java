package yubi.server.base.dto;

import yubi.core.entity.RelSubjectColumns;
import yubi.core.entity.RelVariableSubject;
import yubi.core.entity.Variable;
import yubi.core.entity.View;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ViewDetailDTO extends View {

    private List<RelSubjectColumns> relSubjectColumns;

    private List<Variable> variables;

    private List<RelVariableSubject> relVariableSubjects;

    public ViewDetailDTO(View view) {
        BeanUtils.copyProperties(view, this);
    }
}
