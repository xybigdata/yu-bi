/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yubi.data.provider.calcite;

import yubi.core.common.ReflectUtils;
import yubi.data.provider.calcite.custom.CustomSqlBetweenOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.fun.SqlBetweenOperator;


@Slf4j
public class SpecialSqlCallConverter {

    /**
     * convert some special sql call to custom sql call
     */
    public static SqlCall convert(SqlCall sqlCall) {
        if (sqlCall == null) {
            return null;
        }
        switch (sqlCall.getOperator().getKind()) {
            case BETWEEN:
                return convertBetween(sqlCall);
            default:
                return sqlCall;
        }
    }

    public static SqlCall convertBetween(SqlCall sqlCall) {
        try {
            SqlBetweenOperator operator = (SqlBetweenOperator) sqlCall.getOperator();
            CustomSqlBetweenOperator customSqlBetweenOperator = new CustomSqlBetweenOperator(operator.flag, operator.isNegated());
            ReflectUtils.setFiledValue(sqlCall, "operator", customSqlBetweenOperator);
        } catch (Exception e) {
            log.error("spacial call convert error", e);
        }
        return sqlCall;
    }

}
