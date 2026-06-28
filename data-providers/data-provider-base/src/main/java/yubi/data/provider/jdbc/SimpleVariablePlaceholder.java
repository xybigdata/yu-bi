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

package yubi.data.provider.jdbc;

import yubi.core.data.provider.ScriptVariable;
import yubi.data.provider.calcite.SqlNodeUtils;
import yubi.data.provider.calcite.custom.SqlSimpleStringLiteral;
import yubi.data.provider.script.ReplacementPair;
import yubi.data.provider.script.VariablePlaceholder;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleVariablePlaceholder extends VariablePlaceholder {

    private ScriptVariable variable;

    private SqlIdentifier identifier;

    public SimpleVariablePlaceholder(ScriptVariable variable, SqlDialect sqlDialect, String originalSqlFragment) {
        super(null, sqlDialect, null, originalSqlFragment);
        this.variable = variable;
    }

    @Override
    public ReplacementPair replacementPair() {
        if (variable == null) {
            return new ReplacementPair(originalSqlFragment, originalSqlFragment);
        }
        return new ReplacementPair(originalSqlFragment, formatValue(variable));
    }

    @Override
    public int getStartPos() {
        return Integer.MAX_VALUE;
    }
}
