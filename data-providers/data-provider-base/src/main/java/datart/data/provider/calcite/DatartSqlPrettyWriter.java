/*
 * Datart
 * <p>
 * Copyright 2021
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

package datart.data.provider.calcite;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

/**
 * Custom SqlPrettyWriter that preserves keyword case (lowercase)
 * instead of the default uppercase behavior.
 * <p>
 * This replaces the Javassist-based ClassTransformer patch which used bytecode
 * manipulation to rewrite {@code SqlPrettyWriter.keyword()} at class-load time.
 * The patched behavior appends keywords as-is without any case conversion,
 * preserving the lowercase form that Calcite uses internally.
 * <p>
 * Implementation note: Since {@code maybeWhitespace}, {@code buf}, and
 * {@code needWhitespaceAfter} are private in Calcite's SqlPrettyWriter,
 * this subclass delegates to {@link #print(String)} for whitespace handling
 * and buffer appending, then replicates the {@code needWhitespaceAfter} logic
 * for the {@link #setNeedWhitespace(boolean)} call.
 */
public class DatartSqlPrettyWriter extends SqlPrettyWriter {

    public DatartSqlPrettyWriter(SqlWriterConfig config) {
        super(config);
    }

    public DatartSqlPrettyWriter(SqlDialect dialect, SqlWriterConfig config) {
        super(dialect, config);
    }

    /**
     * Outputs the keyword as-is without case conversion.
     * <p>
     * The default SqlPrettyWriter.keyword() uppercases keywords (or lowercases
     * if configured). This override preserves the original case, matching the
     * behavior of the former Javassist ClassTransformer patch.
     *
     * @param s the keyword string to output
     */
    @Override
    public void keyword(String s) {
        // print(s) handles: maybeWhitespace(s) + buf.append(s)
        print(s);
        if (!s.isEmpty()) {
            // Replicate the private needWhitespaceAfter(s) logic:
            // whitespace is needed after a keyword unless it is "(", "[", or "."
            setNeedWhitespace(!s.equals("(") && !s.equals("[") && !s.equals("."));
        }
    }
}
