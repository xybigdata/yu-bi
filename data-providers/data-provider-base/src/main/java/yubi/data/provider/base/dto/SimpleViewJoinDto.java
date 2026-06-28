package yubi.data.provider.base.dto;

import yubi.core.data.provider.sql.FilterOperator;
import lombok.Data;
import org.apache.calcite.sql.JoinType;

import java.util.List;

@Data
public class SimpleViewJoinDto {

    private JoinType joinType;

    private String tableName;

    private List<FilterOperator> conditions;

}
