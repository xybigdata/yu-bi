package yubi.core.data.provider.sql;

import yubi.core.data.provider.StdSqlOperator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FunctionOperator implements Operator {

    private StdSqlOperator function;

    private List<FunArg> args;

}
