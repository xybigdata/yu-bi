///*
// * YuBi
// * <p>
// * Copyright 2021 (originally Datart by running-elephant)
// * Copyright 2024-2026 YuBi Contributors
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package yubi.data.provider.jdbc;
//
//import yubi.core.data.provider.ScriptVariable;
//import yubi.data.provider.script.ReplacementPair;
//import yubi.data.provider.script.VariablePlaceholder;
//import org.apache.calcite.sql.SqlCall;
//import org.apache.calcite.sql.SqlDialect;
//
//public class FalseVariablePlaceholder extends VariablePlaceholder {
//
//    public FalseVariablePlaceholder(ScriptVariable variable, SqlDialect sqlDialect, SqlCall sqlCall, String originalSqlFragment) {
//        super(variable, sqlDialect, sqlCall, originalSqlFragment);
//    }
//
//    public FalseVariablePlaceholder(String originalSqlFragment) {
//        this(null, null, null, originalSqlFragment);
//    }
//
//    @Override
//    public ReplacementPair replacementPair() {
//        return new ReplacementPair(originalSqlFragment, SqlScriptRender.FALSE_CONDITION);
//    }
//}
